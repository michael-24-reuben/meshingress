package dev.mrk.meshingress.dispatch.resolver;

import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class TypedJsonArgumentBinder {

    public Object bind(String method, String source, JsonNode value, Class<?> targetType, ObjectMapper objectMapper) {
        if (JsonNode.class.isAssignableFrom(targetType)) {
            return bindJsonNode(method, source, value, targetType);
        }
        if (targetType.equals(String.class)) {
            if (!value.isTextual()) {
                throw invalid(method, source, "must be a string");
            }
            return value.asString();
        }
        if (targetType.equals(Integer.class) || targetType.equals(Integer.TYPE)) {
            if (!value.isNumber()) {
                throw invalid(method, source, "must be a number");
            }
            return value.asInt();
        }
        if (targetType.equals(Long.class) || targetType.equals(Long.TYPE)) {
            if (!value.isNumber()) {
                throw invalid(method, source, "must be a number");
            }
            return value.asLong();
        }
        if (targetType.equals(Double.class) || targetType.equals(Double.TYPE)) {
            if (!value.isNumber()) {
                throw invalid(method, source, "must be a number");
            }
            return value.asDouble();
        }
        if (targetType.equals(Boolean.class) || targetType.equals(Boolean.TYPE)) {
            if (!value.isBoolean()) {
                throw invalid(method, source, "must be a boolean");
            }
            return value.asBoolean();
        }
        try {
            return objectMapper.treeToValue(value, targetType);
        } catch (Exception exception) {
            throw new JsonRpcException(
                    JsonRpcErrorCodes.INVALID_PARAMS,
                    "Unable to bind MCP method '%s' parameter '%s' to %s"
                            .formatted(method, source, targetType.getName())
            );
        }
    }

    private Object bindJsonNode(String method, String source, JsonNode value, Class<?> targetType) {
        if (targetType.equals(ObjectNode.class) && !value.isObject()) {
            throw invalid(method, source, "must be an object");
        }
        if (targetType.equals(ArrayNode.class) && !value.isArray()) {
            throw invalid(method, source, "must be an array");
        }
        if (!targetType.isInstance(value)) {
            throw invalid(method, source, "cannot be assigned to " + targetType.getName());
        }
        return value;
    }

    private JsonRpcException invalid(String method, String source, String message) {
        return new JsonRpcException(
                JsonRpcErrorCodes.INVALID_PARAMS,
                "MCP method '%s' parameter '%s' %s".formatted(method, source, message)
        );
    }
}
