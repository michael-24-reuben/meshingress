package dev.mrk.meshingress.mcp.tools.annotation;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityDecision;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;
import dev.mrk.meshingress.api.tools.annotation.availability.ToolAvailabilityContext;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunction;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.cache.McpCacheManager;
import dev.mrk.meshingress.route.api.McpDispatchException;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class AnnotatedMcpToolHandler implements McpToolHandler {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedMcpToolHandler.class);

    private final Object bean;
    private final AnnotatedMcpTool tool;
    private final AnnotatedMcpFunction function;
    private final ObjectMapper objectMapper;
    private final TypedJsonArgumentBinder argumentBinder;
    private final McpCacheManager cacheManager;

    public AnnotatedMcpToolHandler(
            Object bean,
            AnnotatedMcpTool tool,
            AnnotatedMcpFunction function,
            ObjectMapper objectMapper,
            TypedJsonArgumentBinder argumentBinder,
            McpCacheManager cacheManager
    ) {
        this.bean = bean;
        this.tool = tool;
        this.function = function;
        this.objectMapper = objectMapper;
        this.argumentBinder = argumentBinder;
        this.cacheManager = cacheManager;
        this.function.method().setAccessible(true);
    }

    @Override
    public McpToolDescriptor descriptor() {
        return tool.descriptor().withFunctions(java.util.List.of(function.descriptor()));
    }

    @Override
    public DispatchExecutionResult call(ObjectNode arguments, McpCallContext context) {
        McpCacheResult cachePolicy = function.method().getAnnotation(McpCacheResult.class);

        AvailabilityDecisions availabilityDecisions = evaluateAvailabilityPolicy(arguments, context);

        if (!availabilityDecisions.allowed()) {
            throw new JsonRpcException(JsonRpcErrorCodes.TOOL_UNAVAILABLE, "Tool function is not available: " + String.join("; ", availabilityDecisions.reasons()));
        }

        return cacheManager.execute(
                cachePolicy,
                tool.descriptor().name(),
                function.descriptor().name(),
                arguments,
                context,
                () -> invoke(arguments, context)
        );
    }

    public AvailabilityDecisions evaluateAvailabilityPolicy(ObjectNode arguments, McpCallContext context) {
        Method method = this.function.method();
        McpConfigureMapping configureMapping = method.getAnnotation(McpConfigureMapping.class);
        McpAvailabilityMode availabilityMode = configureMapping == null
                ? McpAvailabilityMode.ALL
                : configureMapping.availabilityMode();

        boolean evaluatedAnyPolicy = false;
        boolean availabilityAllowed = availabilityMode == McpAvailabilityMode.ALL;
        List<String> availabilityReasons = new ArrayList<>();
        Map<String, Object> availabilityMetadata = new HashMap<>();


        for (Annotation methodAnnotation : method.getAnnotations()) {
            Class<? extends Annotation> annotationType = methodAnnotation.annotationType();

            McpFunctionAvailabilityPolicy policyAnnotation = annotationType.getAnnotation(McpFunctionAvailabilityPolicy.class);
            if (policyAnnotation == null) {
                continue;
            }

            try {
                Class<? extends McpAvailabilityPolicy<? extends Annotation>> policyClass = policyAnnotation.value();
                McpAvailabilityPolicy<?> policy = policyClass.getDeclaredConstructor().newInstance();

                ToolAvailabilityContext toolContext = new ToolAvailabilityContext(tool.name(), function.name(), arguments, context, Map.of());
                AvailabilityDecision evaluation = evaluatePolicyReflectively(policy, methodAnnotation, toolContext);

                evaluatedAnyPolicy = true;

                if (availabilityMode == McpAvailabilityMode.ALL) {
                    availabilityAllowed = availabilityAllowed && evaluation.allowed();
                } else {
                    availabilityAllowed = availabilityAllowed || evaluation.allowed();
                }

                availabilityReasons.add(evaluation.reason());
                availabilityMetadata.put(annotationType.getSimpleName(), evaluation.metadata());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        if (!evaluatedAnyPolicy) {
            return new AvailabilityDecisions(true, List.of("no availability policies declared"), Map.of());
        }
        return new AvailabilityDecisions(availabilityAllowed, availabilityReasons, availabilityMetadata);
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> AvailabilityDecision evaluatePolicyReflectively(
            McpAvailabilityPolicy<?> policy,
            Annotation annotation,
            ToolAvailabilityContext context
    ) {
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(annotation, "annotation must not be null");

        try {
            return ((McpAvailabilityPolicy<A>) policy).evaluate((A) annotation, context, McpAvailabilityPolicy.PolicyEvaluationState.INVOKE_TOOL);
        } catch (ClassCastException ex) {
            throw new IllegalStateException(
                    "Availability policy " + policy.getClass().getName() + " is not compatible with annotation @" + annotation.annotationType().getName(),
                    ex
            );
        }
    }

    private DispatchExecutionResult invoke(ObjectNode arguments, McpCallContext context) {
        try {
            Object result = function.method().invoke(bean, invocationArguments(arguments, context));
            return adaptResult(result);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof JsonRpcException jsonRpcException) {
                throw jsonRpcException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Annotated MCP tool failed: " + cause.getMessage(), cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke annotated MCP tool: " + function.method(), exception);
        }
    }

    private Object[] invocationArguments(ObjectNode arguments, McpCallContext context) {
        Parameter[] parameters = function.method().getParameters();
        Object[] values = new Object[parameters.length];
        int bindableIndex = 0;
        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            if (McpCallContext.class.isAssignableFrom(parameter.getType())) {
                values[index] = context;
                continue;
            }
            if (McpProgressReporter.class.equals(parameter.getType())) {
                values[index] = context.progressReporter();
                continue;
            }

            if (bindableIndex >= function.parameters().size()) {
                throw newJsonRpcException(
                        JsonRpcErrorCodes.INVALID_PARAMS,
                        "Annotated MCP tool parameter is not bindable: " + parameter.getName()
                );
            }
            AnnotatedMcpFunctionParam param = function.parameters().get(bindableIndex);
            bindableIndex++;
            values[index] = bindArgument(arguments, param);
        }
        return values;
    }

    private Object bindArgument(ObjectNode arguments, AnnotatedMcpFunctionParam param) {
        JsonNode value = param.name().equals("args") ? arguments : arguments.path(param.name());

        if ((value.isMissingNode() || value.isNull()) && !param.required()) {
            if (param.parameterType().isPrimitive()) {
                throw newJsonRpcException(
                        JsonRpcErrorCodes.INVALID_PARAMS,
                        "Missing required primitive tool argument: " + param.name()
                );
            }
            return null;
        }
        try {
            return argumentBinder.bind(function.path(), param.name(), value, param.bindType(), objectMapper);
        } catch (McpDispatchException exception) {
            throw newJsonRpcException(exception.code(), exception.getMessage());
        }
    }

    private static @NonNull JsonRpcException newJsonRpcException(int code, String message) {
        JsonRpcException jsonRpcException = new JsonRpcException(code, message);
        log.error(jsonRpcException.getMessage(), jsonRpcException);
        return jsonRpcException;
    }

    private DispatchExecutionResult adaptResult(Object result) {
        if (result instanceof DispatchExecutionResult toolExecutionResult) {
            return toolExecutionResult;
        }
        JsonNode structured = result instanceof JsonNode jsonNode ? jsonNode : objectMapper.valueToTree(result);
        String text = structured == null || structured.isNull() ? "" : structured.toString();
        return DispatchExecutionResult.builder()
                .text(text)
                .structuredContent(structured)
                .build();
    }

    public record AvailabilityDecisions(boolean allowed, List<String> reasons, Map<String, Object> metadata) {
    }
}
