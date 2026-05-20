package dev.mrk.meshingress.dispatch.invoker;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.dispatch.resolver.McpDispatchArgumentResolver;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;

public class McpHandlerMethodInvoker {

    private final ObjectMapper objectMapper;
    private final List<McpDispatchArgumentResolver> argumentResolvers;
    private final McpReturnValueAdapter returnValueAdapter;

    public McpHandlerMethodInvoker(
            ObjectMapper objectMapper,
            List<McpDispatchArgumentResolver> argumentResolvers,
            McpReturnValueAdapter returnValueAdapter
    ) {
        this.objectMapper = objectMapper;
        this.argumentResolvers = List.copyOf(argumentResolvers);
        this.returnValueAdapter = returnValueAdapter;
    }

    public JsonNode invoke(McpDispatchHandlerMethod handler, JsonNode params, McpCallContext context) {
        try {
            Object result = handler.method().invoke(
                    handler.bean(),
                    resolveArguments(handler, params, context)
            );
            return returnValueAdapter.toJsonNode(result, objectMapper);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof JsonRpcException jsonRpcException) {
                throw jsonRpcException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch invocation failed");
        } catch (IllegalAccessException exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch method is not accessible");
        }
    }

    private Object[] resolveArguments(McpDispatchHandlerMethod handler, JsonNode params, McpCallContext context) {
        Parameter[] parameters = handler.method().getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            int parameterIndex = i;
            Parameter parameter = parameters[i];
            McpDispatchArgumentResolver resolver = argumentResolvers.stream()
                    .filter(candidate -> candidate.supports(parameter))
                    .findFirst()
                    .orElseThrow(() -> new JsonRpcException(
                            JsonRpcErrorCodes.INVALID_PARAMS,
                            "No MCP dispatch argument resolver for parameter %d on method '%s'"
                                    .formatted(parameterIndex, handler.methodName())
                    ));
            arguments[i] = resolver.resolve(handler.methodName(), params, context, parameter, objectMapper);
        }
        return arguments;
    }
}
