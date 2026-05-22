package dev.mrk.meshingress.tools.framework.scanner;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunction;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.schema.McpJsonSchemaProvider;
import dev.mrk.meshingress.scopes.McpToolScope;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.*;
import java.util.*;

public class McpToolAnnotationScanner {
    private static final Logger log = LoggerFactory.getLogger(McpToolAnnotationScanner.class);
    private static final Class<?> defaultParamImplementationClass = Void.class;

    private final ObjectMapper objectMapper;

    public McpToolAnnotationScanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AnnotatedMcpTool> scan(Collection<Class<?>> toolClasses) {
        return toolClasses.stream()
                .map(this::scan)
                .toList();
    }

    public AnnotatedMcpTool scan(@NonNull Class<?> toolClass) {
        McpTool tool = toolClass.getAnnotation(McpTool.class);
        if (tool == null) {
            throw new IllegalStateException("Annotated MCP tool requires @McpTool: " + toolClass.getName());
        }

        String mapping = mapping(toolClass);
        List<AnnotatedMcpFunction> functions = scanFunctions(toolClass, tool, mapping);
        AnnotatedMcpFunction defaultFunction = defaultFunction(tool, functions);
        ObjectNode inputSchema = defaultFunction == null
                ? emptyObjectSchema(tool.description())
                : defaultFunction.inputSchema();
        ObjectNode annotations = scopesJson(toolClass.getAnnotation(McpToolScopes.class));
        McpToolDescriptor descriptor = newMcpToolDescriptor(tool, inputSchema, annotations);

        return new AnnotatedMcpTool(toolClass, mapping, tool.invocationName(), descriptor, functions);
    }

    @Contract("_, _, _ -> new")
    private @NonNull McpToolDescriptor newMcpToolDescriptor(@NonNull McpTool tool, ObjectNode inputSchema, ObjectNode annotations) {
        String handlerKey = !tool.handlerKey().isBlank()
                ? tool.handlerKey()
                : (!tool.invocationName().isBlank() ? tool.invocationName() : tool.value());

        return new McpToolDescriptor(
                tool.value(),
                blankToNull(tool.title()),
                tool.description(),
                tool.version(),
                tool.enabled(),
                tool.visibility(),
                handlerKey,
                inputSchema,
                null,
                annotations,
                tool.dynamic()
        );
    }

    private List<AnnotatedMcpFunction> scanFunctions(@NonNull Class<?> toolClass, McpTool tool, String mapping) {
        Map<String, AnnotatedMcpFunction> functions = new LinkedHashMap<>();
        for (Method method : toolClass.getDeclaredMethods()) {
            McpFunction function = method.getAnnotation(McpFunction.class);
            if (function == null) {
                continue;
            }
            validateFunction(method);
            String name = function.value().isBlank() ? method.getName() : function.value();
            String path = compose(mapping, name);
            AnnotatedMcpFunction annotatedFunction = new AnnotatedMcpFunction(
                    name,
                    path,
                    function.title(),
                    function.description(),
                    evalToolFunctionEnabled(tool, function), // Function is only enabled if both the tool and function are enabled
                    evalToolFunctionVisibility(tool, function),
                    method,
                    inputSchemaFor(method, function.description()),
                    functionScopes(toolClass, method),
                    parametersFor(method)
            );
            AnnotatedMcpFunction existing = functions.putIfAbsent(path, annotatedFunction);
            if (existing != null) {
                throw new IllegalStateException("Duplicate MCP tool function mapping '%s' on %s and %s"
                        .formatted(path, existing.method().toGenericString(), method.toGenericString()));
            }
        }
        return List.copyOf(functions.values());
    }

    private static boolean evalToolFunctionEnabled(@NonNull McpTool tool, McpFunction function) {
        if (!tool.enabled() && function.enabled()) // Logs failure state to the output stream
            log.error("Function {} is enabled on {} but the tool is disabled. Defaulting to disabled.", function, tool);
        return tool.enabled() && function.enabled();
    }

    private static @NonNull ToolVisibility evalToolFunctionVisibility(@NonNull McpTool tool, @NonNull McpFunction function) {
        boolean canContainVisibility = tool.visibility().canContainVisibility(function.visibility());
        if (!canContainVisibility) // Logs failure state to the output stream
            log.error("Function visibility '{}' on {} is not compatible with tool visibility '{}' on {}. Defaulting to tool visibility.",
                    function.visibility(), function, tool.visibility(), tool);

        return canContainVisibility ? function.visibility() : tool.visibility();
    }

    private void validateFunction(@NonNull Method method) {
        if (method.isBridge() || method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("MCP tool function must be a concrete instance method: " + method);
        }
        if (method.getReturnType().equals(Void.TYPE)) {
            throw new IllegalStateException("MCP tool function must not return void: " + method);
        }
        List<Parameter> functionParameters = bindableParameters(method);
        boolean canInferArgsParam = functionParameters.size() == 1
                && !functionParameters.getFirst().isAnnotationPresent(McpFunctionParam.class);
        for (Parameter parameter : functionParameters) {
            if (McpCallContext.class.isAssignableFrom(parameter.getType())) {
                continue;
            }
            if (!canInferArgsParam && !parameter.isAnnotationPresent(McpFunctionParam.class)) {
                throw new IllegalStateException("MCP tool function parameter requires @McpFunctionParam or McpCallContext type: "
                        + method.toGenericString());
            }
        }
    }

    private List<AnnotatedMcpFunctionParam> parametersFor(Method method) {
        List<AnnotatedMcpFunctionParam> params = new ArrayList<>();
        List<Parameter> functionParameters = bindableParameters(method);
        boolean inferSingleArgsParam = functionParameters.size() == 1
                && !functionParameters.getFirst().isAnnotationPresent(McpFunctionParam.class);
        for (Parameter parameter : functionParameters) {
            McpFunctionParam annotation = parameter.getAnnotation(McpFunctionParam.class);
            if (annotation == null && inferSingleArgsParam) {
                params.add(new AnnotatedMcpFunctionParam(
                        "args",
                        "",
                        true,
                        parameter.getType(),
                        parameter.getType(),
                        parameter
                ));
                continue;
            }
            Class<?> bindType = getAClass(method, parameter, annotation);
            params.add(new AnnotatedMcpFunctionParam(
                    annotation != null ? annotation.value() : parameter.getName(),
                    annotation != null ? annotation.description() : "",
                    annotation == null || annotation.required(),
                    parameter.getType(),
                    bindType,
                    parameter
            ));
        }
        return params;
    }

    private static @NonNull Class<?> getAClass(Method method, Parameter parameter, McpFunctionParam annotation) {
        Class<?> implementation = annotation != null ? annotation.implementation() : defaultParamImplementationClass;
        Class<?> bindType = implementation.equals(defaultParamImplementationClass)
                ? parameter.getType()
                : implementation;
        if (!parameter.getType().isAssignableFrom(bindType)) {
            throw new IllegalStateException("MCP tool function implementation %s is not assignable to %s on %s"
                    .formatted(bindType.getName(), parameter.getType().getName(), method.toGenericString()));
        }
        return bindType;
    }

    private List<Parameter> bindableParameters(Method method) {
        List<Parameter> parameters = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (!McpCallContext.class.isAssignableFrom(parameter.getType())) {
                parameters.add(parameter);
            }
        }
        return parameters;
    }

    private ObjectNode inputSchemaFor(Method method, String fallbackDescription) {
        McpInputSchema methodSchema = method.getAnnotation(McpInputSchema.class);
        if (methodSchema != null && !methodSchema.provider().equals(McpJsonSchemaProvider.class)) {
            return schemaFromProvider(methodSchema.provider());
        }

        List<AnnotatedMcpFunctionParam> params = parametersFor(method);
        AnnotatedMcpFunctionParam argsParam = params.stream()
                .filter(param -> param.name().equals("args"))
                .findFirst()
                .orElse(null);
        if (argsParam != null && !JsonNode.class.isAssignableFrom(argsParam.bindType())) {
            return objectSchemaFor(argsParam.bindType(), argsParam.description());
        }

        ObjectNode schema = emptyObjectSchema(fallbackDescription);
        ObjectNode properties = objectMapper.createObjectNode();
        ArrayNode required = objectMapper.createArrayNode();
        for (AnnotatedMcpFunctionParam param : params) {
            ObjectNode property = schemaForType(param.bindType(), param.description());
            properties.set(param.name(), property);
            if (param.required()) {
                required.add(param.name());
            }
        }
        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
        return schema;
    }

    private ObjectNode objectSchemaFor(Class<?> type, String description) {
        McpInputSchema inputSchema = type.getAnnotation(McpInputSchema.class);
        if (inputSchema != null && !inputSchema.provider().equals(McpJsonSchemaProvider.class)) {
            return schemaFromProvider(inputSchema.provider());
        }

        ObjectNode schema = emptyObjectSchema(!description.isBlank()
                ? description
                : inputSchema == null ? "" : inputSchema.description());
        ObjectNode properties = objectMapper.createObjectNode();
        ArrayNode required = objectMapper.createArrayNode();

        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                McpInputField field = component.getAnnotation(McpInputField.class);
                String name = fieldName(component.getName(), field);
                properties.set(name, schemaForType(component.getType(), field == null ? "" : field.description()));
                if (field == null || field.required()) {
                    required.add(name);
                }
            }
        } else {
            for (Field declaredField : type.getDeclaredFields()) {
                if (Modifier.isStatic(declaredField.getModifiers()) || declaredField.isSynthetic()) {
                    continue;
                }
                McpInputField field = declaredField.getAnnotation(McpInputField.class);
                String name = fieldName(declaredField.getName(), field);
                properties.set(name, schemaForType(declaredField.getType(), field == null ? "" : field.description()));
                if (field == null || field.required()) {
                    required.add(name);
                }
            }
        }

        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
        return schema;
    }

    private ObjectNode schemaForType(Class<?> type, String description) {
        ObjectNode schema = objectMapper.createObjectNode();
        if (type.equals(String.class) || type.equals(Character.class) || type.equals(Character.TYPE)) {
            schema.put("type", "string");
        } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)
                || type.equals(Long.class) || type.equals(Long.TYPE)
                || type.equals(Short.class) || type.equals(Short.TYPE)
                || type.equals(Byte.class) || type.equals(Byte.TYPE)) {
            schema.put("type", "integer");
        } else if (Number.class.isAssignableFrom(type)
                || type.equals(Double.TYPE)
                || type.equals(Float.TYPE)) {
            schema.put("type", "number");
        } else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
            schema.put("type", "boolean");
        } else if (type.isEnum()) {
            schema.put("type", "string");
            ArrayNode values = objectMapper.createArrayNode();
            for (Object constant : type.getEnumConstants()) {
                values.add(((Enum<?>) constant).name());
            }
            schema.set("enum", values);
        } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            schema.put("type", "array");
        } else if (JsonNode.class.isAssignableFrom(type)) {
            schema.put("type", ObjectNode.class.isAssignableFrom(type) ? "object" : "object");
        } else {
            schema.put("type", "object");
        }
        if (description != null && !description.isBlank()) {
            schema.put("description", description);
        }
        return schema;
    }

    private ObjectNode schemaFromProvider(Class<? extends McpJsonSchemaProvider> providerType) {
        try {
            return providerType.getDeclaredConstructor().newInstance().schema(objectMapper);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create MCP schema provider: " + providerType.getName(), exception);
        }
    }

    private ObjectNode functionScopes(Class<?> toolClass, Method method) {
        McpToolScopes functionScopes = method.getAnnotation(McpToolScopes.class);
        return scopesJson(functionScopes == null ? toolClass.getAnnotation(McpToolScopes.class) : functionScopes);
    }

    private ObjectNode scopesJson(McpToolScopes scopes) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode values = objectMapper.createArrayNode();
        if (scopes != null) {
            for (McpToolScope scope : scopes.value()) {
                values.add(scope.name());
            }
        }
        node.set("scopes", values);
        return node;
    }

    private ObjectNode emptyObjectSchema(String description) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        if (description != null && !description.isBlank()) {
            schema.put("description", description);
        }
        return schema;
    }

    private AnnotatedMcpFunction defaultFunction(McpTool tool, List<AnnotatedMcpFunction> functions) {
        if (functions.isEmpty()) {
            return null;
        }
        if (tool.invocationName().isBlank()) {
            return functions.getFirst();
        }
        return functions.stream()
                .filter(function -> tool.invocationName().equals(function.name())
                        || tool.invocationName().equals(function.path())
                        || tool.invocationName().equals(tool.value()))
                .findFirst()
                .orElse(functions.getFirst());
    }

    private String mapping(Class<?> toolClass) {
        McpToolMapping mapping = toolClass.getAnnotation(McpToolMapping.class);
        return mapping == null ? "" : normalize(mapping.value(), true);
    }

    private String compose(String mapping, String function) {
        String left = normalize(mapping, true);
        String right = normalize(function, false);
        return left.isBlank() ? right : left + "/" + right;
    }

    private String normalize(String value, boolean allowBlank) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!allowBlank && normalized.isBlank()) {
            throw new IllegalStateException("MCP tool function segment must not be blank");
        }
        if (normalized.contains("//")) {
            throw new IllegalStateException("MCP tool path segment must not contain empty path parts: " + value);
        }
        return normalized;
    }

    private String fieldName(String fallback, McpInputField field) {
        if (field != null && !field.value().isBlank()) {
            return field.value();
        }
        return fallback;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
