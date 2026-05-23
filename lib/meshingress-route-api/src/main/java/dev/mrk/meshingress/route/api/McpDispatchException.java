package dev.mrk.meshingress.route.api;

import tools.jackson.databind.JsonNode;

public class McpDispatchException extends RuntimeException {

    private final int code;
    private final JsonNode data;

    public McpDispatchException(int code, String message) {
        this(code, message, null);
    }

    public McpDispatchException(int code, String message, JsonNode data) {
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
