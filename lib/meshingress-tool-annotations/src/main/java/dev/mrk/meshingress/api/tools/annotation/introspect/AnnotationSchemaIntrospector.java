package dev.mrk.meshingress.api.tools.annotation.introspect;

import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AnnotationSchemaIntrospector {

    private final ObjectMapper objectMapper;

    public AnnotationSchemaIntrospector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode describeAnnotation(Class<? extends Annotation> annotationType) {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("annotation", annotationType.getName());
        root.put("simpleName", annotationType.getSimpleName());

        ArrayNode targets = objectMapper.createArrayNode();

        Target target = annotationType.getAnnotation(Target.class);
        if (target != null) {
            Arrays.stream(target.value())
                    .map(ElementType::name)
                    .forEach(targets::add);
        }

        root.set("targets", targets);

        ArrayNode elements = objectMapper.createArrayNode();

        Arrays.stream(annotationType.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .forEach(method -> elements.add(describeElement(method)));

        root.set("elements", elements);

        return root;
    }

    public ObjectNode describeAnnotatedClass(Class<?> type, Class<? extends Annotation> annotationType) {
        Annotation annotation = type.getAnnotation(annotationType);

        if (annotation == null) {
            throw new IllegalArgumentException(
                    type.getName() + " is not annotated with " + annotationType.getName()
            );
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("className", type.getName());
        root.set("annotation", describeAnnotationInstance(annotation));

        return root;
    }

    private ObjectNode describeElement(Method method) {
        ObjectNode node = objectMapper.createObjectNode();

        Class<?> returnType = method.getReturnType();
        Object defaultValue = method.getDefaultValue();

        node.put("name", method.getName());
        node.put("type", method.getReturnType().getName());
        node.put("required", defaultValue == null);
        node.set("annotations", describeAnnotationInstances(method.getDeclaredAnnotations()));

        if (defaultValue == null) {
            node.set("defaultValue", objectMapper.nullNode());
        } else {
            node.set("defaultValue", annotationValueToJson(defaultValue));
        }

        appendTypeMetadata(node, returnType);

        return node;
    }

    private ArrayNode describeAnnotationInstances(Annotation[] annotations) {
        ArrayNode array = objectMapper.createArrayNode();

        Arrays.stream(annotations)
                .sorted(Comparator.comparing(annotation -> annotation.annotationType().getName()))
                .forEach(annotation -> array.add(describeAnnotationInstance(annotation)));

        return array;
    }

    private JsonNode annotationValueToJson(Object value) {
        if (value == null) {
            return objectMapper.nullNode();
        }

        if (value instanceof Class<?> clazz) {
            return objectMapper.valueToTree(clazz.getName());
        }

        if (value instanceof Enum<?> enumValue) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("enumType", enumValue.getDeclaringClass().getName());
            node.put("name", enumValue.name());
            return node;
        }

        if (value instanceof Annotation annotation) {
            return describeAnnotationInstance(annotation);
        }

        Class<?> valueType = value.getClass();

        if (valueType.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            int length = java.lang.reflect.Array.getLength(value);

            for (int i = 0; i < length; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                array.add(annotationValueToJson(item));
            }

            return array;
        }

        return objectMapper.valueToTree(value);
    }

    private @NonNull ObjectNode describeAnnotationInstance(@NonNull Annotation annotation) {
        ObjectNode node = objectMapper.createObjectNode();

        Class<? extends Annotation> annotationType = annotation.annotationType();

        node.put("name", annotationType.getName());
        node.put("simpleName", annotationType.getSimpleName());

        ObjectNode values = objectMapper.createObjectNode();

        Arrays.stream(annotationType.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .forEach(method -> {
                    try {
                        Object value = method.invoke(annotation);
                        values.set(method.getName(), annotationValueToJson(value));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(
                                "Failed to read annotation member: " + annotationType.getName() + "." + method.getName(),
                                e
                        );
                    }
                });

        node.set("values", values);
        return node;
    }

    private void appendTypeMetadata(ObjectNode node, Class<?> type) {
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();

            node.put("array", true);
            node.put("componentType", componentType.getName());
            node.put("componentKind", kindOf(componentType));

            if (componentType.isEnum()) {
                node.set("enumValues", enumValuesToJson(componentType));
            }

            if (componentType.isAnnotation()) {
                node.set(
                        "annotationSchema",
                        describeAnnotation(componentType.asSubclass(Annotation.class))
                );
            }

            return;
        }

        node.put("array", false);
        node.put("kind", kindOf(type));

        if (type.isEnum()) {
            node.set("enumValues", enumValuesToJson(type));
        }

        if (type.isAnnotation()) {
            node.set(
                    "annotationSchema",
                    describeAnnotation(type.asSubclass(Annotation.class))
            );
        }
    }

    private String kindOf(Class<?> type) {
        if (type.isEnum()) {
            return "enum";
        }

        if (type.isAnnotation()) {
            return "annotation";
        }

        if (type == String.class) {
            return "string";
        }

        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }

        if (type == int.class || type == Integer.class ||
                type == long.class || type == Long.class ||
                type == double.class || type == Double.class ||
                type == float.class || type == Float.class ||
                type == short.class || type == Short.class ||
                type == byte.class || type == Byte.class) {
            return "number";
        }

        if (type == Class.class) {
            return "class";
        }

        return "object";
    }

    private JsonNode valueToJson(Object value) {
        if (value == null) {
            return objectMapper.nullNode();
        }

        Class<?> valueType = value.getClass();

        if (valueType.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            int length = Array.getLength(value);

            for (int i = 0; i < length; i++) {
                array.add(valueToJson(Array.get(value, i)));
            }

            return array;
        }

        if (value instanceof Enum<?> enumValue) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", enumValue.name());
            node.put("enumType", enumValue.getDeclaringClass().getName());
            node.set("fields", enumFieldsToJson(enumValue));
            return node;
        }

        if (value instanceof Class<?> clazz) {
            return objectMapper.valueToTree(clazz.getName());
        }

        if (value instanceof Annotation annotation) {
            return annotationInstanceToJson(annotation);
        }

        return objectMapper.valueToTree(value);
    }

    private ObjectNode annotationInstanceToJson(Annotation annotation) {
        ObjectNode node = objectMapper.createObjectNode();

        node.put("annotation", annotation.annotationType().getName());
        node.put("simpleName", annotation.annotationType().getSimpleName());

        ObjectNode values = objectMapper.createObjectNode();

        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                values.set(method.getName(), valueToJson(method.invoke(annotation)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Failed to read annotation value: "
                                + annotation.annotationType().getName()
                                + "."
                                + method.getName(),
                        e
                );
            }
        }

        node.set("values", values);
        return node;
    }

    private ArrayNode describeAnnotations(Annotation[] annotations) {
        ArrayNode array = objectMapper.createArrayNode();

        Arrays.stream(annotations)
                .sorted(Comparator.comparing(a -> a.annotationType().getName()))
                .map(this::annotationInstanceToJson)
                .forEach(array::add);

        return array;
    }

    private ArrayNode enumValuesToJson(Class<?> enumType) {
        ArrayNode values = objectMapper.createArrayNode();

        Object[] constants = enumType.getEnumConstants();

        if (constants == null) {
            return values;
        }

        for (Object constant : constants) {
            Enum<?> enumValue = (Enum<?>) constant;

            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", enumValue.name());
            node.put("ordinal", enumValue.ordinal());
            node.set("fields", enumFieldsToJson(enumValue));

            values.add(node);
        }

        return values;
    }

    private ObjectNode enumFieldsToJson(Enum<?> enumValue) {
        ObjectNode fields = objectMapper.createObjectNode();

        Method[] methods = enumValue.getDeclaringClass().getMethods();

        Arrays.stream(methods)
                .filter(this::isEnumMetadataMethod)
                .sorted(Comparator.comparing(Method::getName))
                .forEach(method -> {
                    try {
                        Object value = method.invoke(enumValue);
                        fields.set(method.getName(), valueToJson(value));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(
                                "Failed to read enum metadata method: "
                                        + enumValue.getDeclaringClass().getName()
                                        + "."
                                        + method.getName(),
                                e
                        );
                    }
                });

        return fields;
    }

    private boolean isEnumMetadataMethod(Method method) {
        if (method.getParameterCount() != 0) {
            return false;
        }

        if (method.getDeclaringClass() == Enum.class) {
            return false;
        }

        if (method.getDeclaringClass() == Object.class) {
            return false;
        }

        String name = method.getName();

        return !name.equals("values")
                && !name.equals("valueOf")
                && !name.equals("$values");
    }

    private String friendlyTypeName(Class<?> type) {
        if (!type.isArray()) {
            return type.getName();
        }

        int dimensions = 0;
        Class<?> componentType = type;

        while (componentType.isArray()) {
            dimensions++;
            componentType = componentType.getComponentType();
        }

        return componentType.getName() + "[]".repeat(dimensions);
    }
}