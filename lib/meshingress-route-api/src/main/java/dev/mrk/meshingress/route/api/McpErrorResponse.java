package dev.mrk.meshingress.route.api;

import java.util.Map;

public record McpErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {

    public McpErrorResponse {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Error code must not be blank");
        }
        message = message == null ? "" : message;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static McpErrorResponse of(String code, String message) {
        return new McpErrorResponse(code, message, Map.of());
    }
}
