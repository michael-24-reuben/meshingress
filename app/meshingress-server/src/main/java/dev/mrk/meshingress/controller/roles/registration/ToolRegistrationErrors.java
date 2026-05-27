package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

final class ToolRegistrationErrors {

    private ToolRegistrationErrors() {
    }

    static JsonRpcException invalidParams(String message, String errorCode) {
        return exception(JsonRpcErrorCodes.INVALID_PARAMS, message, errorCode);
    }

    static JsonRpcException forbidden(String message, String errorCode) {
        return exception(JsonRpcErrorCodes.FORBIDDEN, message, errorCode);
    }

    static JsonRpcException exception(int rpcCode, String message, String errorCode) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("errorCode", errorCode);
        return new JsonRpcException(rpcCode, message, data);
    }
}
