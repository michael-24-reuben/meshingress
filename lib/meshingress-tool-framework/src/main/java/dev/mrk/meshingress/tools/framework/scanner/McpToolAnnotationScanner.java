package dev.mrk.meshingress.tools.framework.scanner;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunction;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.schema.McpJsonSchemaProvider;
import dev.mrk.meshingress.scopes.McpToolScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class McpToolAnnotationScanner {
    private static final Logger log = LoggerFactory.getLogger(McpToolAnnotationScanner.class);
    private static final Class<?> defaultParamImplementationClass = Void.class;

    private final ObjectMapper objectMapper;
    private static final String QUALIFIED_TOOL_NAME_REGEX = "[a-z][a-z0-9]*(\\.[a-z0-9]+)*";

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

        } else if (!tool.value().isBlank() && !tool.value().matches(QUALIFIED_TOOL_NAME_REGEX)) {
            throw new IllegalStateException("Mcp tool '%s' requires valid @McpTool annotation: '%s'".formatted(toolClass.getName(), tool.value()));

        } else if (!tool.invocationName().isBlank() && !tool.invocationName().matches(QUALIFIED_TOOL_NAME_REGEX)) {
            throw new IllegalStateException("Mcp tool '%s' requires valid @McpTool annotation: '%s'".formatted(toolClass.getName(), tool.invocationName()));
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

            McpConfigureMapping functionMapping = method.getAnnotation(McpConfigureMapping.class);
            if (functionMapping == null) {
                functionMapping = McpToolDefaults.class.getAnnotation(McpConfigureMapping.class);
            }

            String name = function.value().isBlank() ? method.getName() : function.value();
            String path = compose(mapping, name);

            List<String> availabilityMessages = new ArrayList<>();
            McpFunctionAvailabilityState availabilityState = evalFunctionAvailability(toolClass, method, name, functionMapping, availabilityMessages);
            McpFunctionAvailability functionAvailabilityPolicy = newMcpFunctionAvailability(tool, function, functionMapping, availabilityState);

            AnnotatedMcpFunction annotatedFunction = new AnnotatedMcpFunction(
                    name,
                    path,
                    function.title(),
                    function.description(),
                    functionAvailabilityPolicy,
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

    /**
     * Determines whether the availability conditions should be considered available based on the presence of conditions and the availability mode.
     * If no conditions provided, the tool is always available unless explicitly disabled. If conditions are provided, the availability is determined
     * by the availability state, which is based on the availability mode (ALL or ANY) and the evaluation of conditions.
     */
    private static boolean isConditionsAvailable(boolean foundAvailabilityCondition, McpFunctionAvailabilityState availabilityState) {
        if (!foundAvailabilityCondition) {
            return availabilityState.isAvailable();
        }
        return true;
    }

    /**
     * Compiles tool validity annotation to see if the function is available.
     * <p>
     * • If any condition fails, the function is unavailable.
     * <p>
     * • If the function mapping mode is ALL, all conditions will be evaluated and messages will be aggregated.
     * <p>
     * • If the function mapping mode is ANY, conditions will be evaluated until the first success, and the function is available.
     */
    private @Nullable McpFunctionAvailabilityState evalFunctionAvailability(
            Class<?> toolClass,
            @NotNull Method method,
            @NotNull String toolFunctionName,
            @NotNull McpConfigureMapping functionMapping,
            List<String> availabilityMessages
    ) {
        McpAvailabilityMode availabilityMode = functionMapping.availabilityMode();
        boolean conditionsAvailable = availabilityMode == McpAvailabilityMode.ALL;
        boolean foundAvailabilityCondition = false;

        for (Annotation methodAnnotation : method.getAnnotations()) {
            Class<? extends Annotation> annotationType = methodAnnotation.annotationType();

            if (!annotationType.isAnnotationPresent(McpFunctionAvailabilityCondition.class)) {
                continue;
            }
            foundAvailabilityCondition = true;

            McpFunctionAvailabilityCondition conditionAnnotation = annotationType.getAnnotation(McpFunctionAvailabilityCondition.class);

            Class<? extends McpAvailabilityCondition<? extends Annotation>> conditionClass = conditionAnnotation.value();

            try {
                McpAvailabilityCondition<?> condition = conditionClass.getDeclaredConstructor().newInstance();

                List<String> result = validateReflectiveAvailabilityCondition(condition, methodAnnotation, new AvailabilityValidationContext(toolClass, method));

                boolean conditionPassed = result.isEmpty();

                if (conditionPassed) {
                    log.info("MCP function {} on {} passed availability condition {}",
                            toolFunctionName, method.toGenericString(), conditionClass.getName());

                    if (availabilityMode == McpAvailabilityMode.ANY) {
                        conditionsAvailable = true;
                        break;
                    }
                } else {
                    availabilityMessages.addAll(result);

                    log.info("MCP function {} on {} is unavailable due to condition {}",
                            toolFunctionName, method.toGenericString(), conditionClass.getName());

                    result.forEach(message -> log.info(" - {}", message));

                    if (availabilityMode == McpAvailabilityMode.ALL) {
                        conditionsAvailable = false;
                        // Do not break if you want ALL to aggregate every failure message.
                        // break; only if you want fail-fast ALL.
                    }
                }

            } catch (ReflectiveOperationException exception) {
                availabilityMessages.add(
                        "Unable to evaluate availability condition " + conditionClass.getName() + ": " + exception.getMessage()
                );

                log.error("Unable to evaluate availability condition {} on {} for MCP function {}: {}",
                        conditionClass.getName(), method.toGenericString(), toolFunctionName, exception.getMessage(), exception);

                if (availabilityMode == McpAvailabilityMode.ALL) {
                    conditionsAvailable = false;
                }
                // If ANY, ignore failures to evaluate conditions, since we only need one success.
            }
        }

        return foundAvailabilityCondition ? new McpFunctionAvailabilityState(availabilityMode, conditionsAvailable) : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateReflectiveAvailabilityCondition(McpAvailabilityCondition<?> condition, Annotation annotation, AvailabilityValidationContext context) {
        return validateAvailabilityCondition(
                (McpAvailabilityCondition<Annotation>) condition,
                annotation,
                context
        );
    }

    private static <A extends Annotation> List<String> validateAvailabilityCondition(@NonNull McpAvailabilityCondition<A> condition, A annotation, AvailabilityValidationContext context) {
        return condition.validate(annotation, context);
    }

    private McpFunctionAvailability newMcpFunctionAvailability(McpTool tool, McpFunction function, McpConfigureMapping functionMapping, McpFunctionAvailabilityState availabilityState) {
        boolean foundAvailabilityCondition = availabilityState == null;

        boolean isFunctionConditionsAvailable = isConditionsAvailable(foundAvailabilityCondition, availabilityState);
        boolean isFunctionStaticEnabled = evalToolFunctionEnabled(tool, function);
        boolean isFunctionAvailable = isFunctionStaticEnabled && isFunctionConditionsAvailable;

        McpAvailabilityConditionResult conditionResult = ;

        return new McpFunctionAvailability(
                isFunctionAvailable,
                evalToolFunctionVisibility(tool, function),
                functionMapping.availabilityMode(),
                isFunctionAvailable,
                List.of(conditionResult)
        );
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
        boolean canInferArgsParam = functionParameters.size() == 1 && !functionParameters.getFirst().isAnnotationPresent(McpFunctionParam.class);

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

    private record McpFunctionAvailabilityState(McpAvailabilityMode availabilityMode, boolean isAvailable) {
    }

    @McpTool()
    private static class McpToolDefaults {
        @McpConfigureMapping
        @McpFunction("main")
        public DispatchExecutionResult main(McpCallContext context) {
            return DispatchExecutionResult.builder().build();
        }
    }
}
