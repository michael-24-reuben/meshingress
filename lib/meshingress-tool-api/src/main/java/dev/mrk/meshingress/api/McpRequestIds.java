package dev.mrk.meshingress.api;

import java.util.Objects;

/** Correlation values for an MCP invocation; none are authentication material. */
public record McpRequestIds(String sessionId, String correlationId, String invocationId, String jsonRpcId) {
    public McpRequestIds {
        sessionId = text(sessionId);
        correlationId = text(correlationId);
        invocationId = Objects.requireNonNullElse(text(invocationId), "");
        jsonRpcId = text(jsonRpcId);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
