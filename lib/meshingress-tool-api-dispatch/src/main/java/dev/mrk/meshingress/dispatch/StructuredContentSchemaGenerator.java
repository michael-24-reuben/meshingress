package dev.mrk.meshingress.dispatch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Generates a stable JSON Schema for a typed {@link StructuredContent} envelope.
 * <p>
 * This deliberately operates on the content class, never on an emitted payload. Anonymous
 * {@link GeneratedJsonContent} is excluded because its field shape belongs to a particular
 * result instance rather than a declared type contract.
 */
public final class StructuredContentSchemaGenerator {

    private StructuredContentSchemaGenerator() {
    }

    public static Optional<ObjectNode> outputSchemaFor(ObjectMapper objectMapper, StructuredContent content) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content instanceof GeneratedJsonContent) {
            return Optional.empty();
        }

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("kind").put("const", content.kind());
        properties.putObject("schema").put("const", content.schema());
        properties.putObject("version").put("const", content.version());
        properties.set("data", schemaForType(objectMapper, content.getClass(), new LinkedHashSet<>()));
        ArrayNode required = schema.putArray("required");
        required.add("kind");
        required.add("schema");
        required.add("version");
        required.add("data");
        return Optional.of(schema);
    }

    private static ObjectNode schemaForType(ObjectMapper objectMapper, Type genericType, Set<Class<?>> ancestors) {
        Class<?> type = rawType(genericType);
        ObjectNode schema = objectMapper.createObjectNode();
        if (type == String.class || type == Character.class || type == Character.TYPE || type.isEnum()) {
            schema.put("type", "string");
            if (type.isEnum()) {
                ArrayNode values = schema.putArray("enum");
                for (Object constant : type.getEnumConstants()) {
                    values.add(((Enum<?>) constant).name());
                }
            }
        } else if (type == Integer.class || type == Integer.TYPE || type == Long.class || type == Long.TYPE
                || type == Short.class || type == Short.TYPE || type == Byte.class || type == Byte.TYPE) {
            schema.put("type", "integer");
        } else if (Number.class.isAssignableFrom(type) || type == Double.TYPE || type == Float.TYPE) {
            schema.put("type", "number");
        } else if (type == Boolean.class || type == Boolean.TYPE) {
            schema.put("type", "boolean");
        } else if (type.isArray() || genericType instanceof GenericArrayType || Collection.class.isAssignableFrom(type)) {
            schema.put("type", "array");
            schema.set("items", schemaForType(objectMapper, elementType(genericType), ancestors));
        } else if (Map.class.isAssignableFrom(type)) {
            schema.put("type", "object");
            schema.set("additionalProperties", schemaForType(objectMapper, mapValueType(genericType), ancestors));
        } else if (type == Object.class || JsonNode.class.isAssignableFrom(type) || Temporal.class.isAssignableFrom(type)) {
            // These types intentionally do not promise a field-level JSON contract.
        } else if (ancestors.contains(type)) {
            // Recursive content models are represented without inventing a finite payload shape.
        } else {
            ancestors.add(type);
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            ObjectNode properties = schema.putObject("properties");
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                properties.set(field.getName(), schemaForType(objectMapper, field.getGenericType(), ancestors));
            }
            ancestors.remove(type);
        }
        return schema;
    }

    private static Type elementType(Type type) {
        if (type instanceof Class<?> classType && classType.isArray()) {
            return classType.getComponentType();
        }
        if (type instanceof GenericArrayType arrayType) {
            return arrayType.getGenericComponentType();
        }
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length == 1) {
            return parameterizedType.getActualTypeArguments()[0];
        }
        return Object.class;
    }

    private static Type mapValueType(Type type) {
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length == 2) {
            return parameterizedType.getActualTypeArguments()[1];
        }
        return Object.class;
    }

    private static Class<?> rawType(Type type) {
        if (type instanceof Class<?> classType) {
            return classType;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawType(parameterizedType.getRawType());
        }
        if (type instanceof GenericArrayType) {
            return Object[].class;
        }
        if (type instanceof WildcardType wildcardType && wildcardType.getUpperBounds().length == 1) {
            return rawType(wildcardType.getUpperBounds()[0]);
        }
        return Object.class;
    }
}
