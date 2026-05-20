package dev.mrk.meshingress.dispatch.resolver;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.dispatch.annotation.McpDispatchParam;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Parameter;

public class McpDispatchParamArgumentResolver implements McpDispatchArgumentResolver {

    private final TypedJsonArgumentBinder binder;

    public McpDispatchParamArgumentResolver(TypedJsonArgumentBinder binder) {
        this.binder = binder;
    }

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(McpDispatchParam.class);
    }

    @Override
    public Object resolve(
            String method,
            JsonNode params,
            McpCallContext context,
            Parameter parameter,
            ObjectMapper objectMapper
    ) {
        McpDispatchParam dispatchParam = parameter.getAnnotation(McpDispatchParam.class);
        String source = dispatchParam.value();
        if (source.equals("context")) {
            return context;
        }
        if (source.equals("method")) {
            return bindDirect(method, source, method, parameter.getType());
        }

        JsonNode sourceValue = resolveJsonSource(source, method, params, dispatchParam.required(), objectMapper);
        Class<?> targetType = dispatchParam.implementation().equals(Void.class)
                ? parameter.getType()
                : dispatchParam.implementation();
        return binder.bind(method, source, sourceValue, targetType, objectMapper);
    }

    private Object bindDirect(String method, String source, Object value, Class<?> targetType) {
        if (!targetType.isInstance(value)) {
            throw new JsonRpcException(
                    JsonRpcErrorCodes.INVALID_PARAMS,
                    "MCP method '%s' parameter '%s' cannot be assigned to %s"
                            .formatted(method, source, targetType.getName())
            );
        }
        return value;
    }

    private JsonNode resolveJsonSource(
            String source,
            String method,
            JsonNode params,
            boolean required,
            ObjectMapper objectMapper
    ) {
        return switch (source) {
            case "params" -> {
                if (isMissing(params) && required) {
                    throw missing(method, source);
                }
                yield params;
            }
            case "args" -> {
                JsonNode arguments = params.path("arguments");
                if (arguments.isMissingNode() || arguments.isNull()) {
                    yield objectMapper.createObjectNode();
                }
                yield arguments;
            }
            case "name" -> {
                JsonNode name = params.path("name");
                if (isMissing(name) && required) {
                    throw missing(method, source);
                }
                if (name.isString() && name.asString("").isBlank() && required) {
                    throw missing(method, source);
                }
                yield name;
            }
            default -> throw new JsonRpcException(
                    JsonRpcErrorCodes.INVALID_PARAMS,
                    "Unsupported MCP method '%s' parameter source '%s'".formatted(method, source)
            );
        };
    }

    private boolean isMissing(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull();
    }

    private JsonRpcException missing(String method, String source) {
        return new JsonRpcException(
                JsonRpcErrorCodes.INVALID_PARAMS,
                "MCP method '%s' parameter '%s' is required".formatted(method, source)
        );
    }
}
