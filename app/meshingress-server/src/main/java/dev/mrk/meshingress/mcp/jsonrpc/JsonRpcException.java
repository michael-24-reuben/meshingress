package dev.mrk.meshingress.mcp.jsonrpc;

import tools.jackson.databind.JsonNode;

public class JsonRpcException extends RuntimeException {

    private final int code;
    private final JsonNode data;

    public JsonRpcException(int code, String message) {
        this(code, message, null);
    }

    public JsonRpcException(int code, String message, JsonNode data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int code() {
        return code;
    }

    public JsonNode data() {
        return data;
    }
}
