package dev.mrk.meshingress.mcp.tools.annotation;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunction;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.route.api.McpDispatchException;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;

public class AnnotatedMcpToolHandler implements McpToolHandler {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedMcpToolHandler.class);

    private final Object bean;
    private final AnnotatedMcpTool tool;
    private final AnnotatedMcpFunction function;
    private final ObjectMapper objectMapper;
    private final TypedJsonArgumentBinder argumentBinder;

    public AnnotatedMcpToolHandler(
            Object bean,
            AnnotatedMcpTool tool,
            ObjectMapper objectMapper,
            TypedJsonArgumentBinder argumentBinder
    ) {
        this.bean = bean;
        this.tool = tool;
        this.function = tool.defaultFunction()
                .orElseThrow(() -> new IllegalStateException(
                        "Annotated MCP tool requires at least one @McpFunction: " + tool.toolClass().getName()
                ));
        this.objectMapper = objectMapper;
        this.argumentBinder = argumentBinder;
        this.function.method().setAccessible(true);
    }

    @Override
    public McpToolDescriptor descriptor() {
        return tool.descriptor();
    }

    @Override
    public DispatchExecutionResult call(ObjectNode arguments, McpCallContext context) {
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
}
