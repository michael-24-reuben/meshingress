package dev.mrk.meshingress.route.api;

import java.util.Map;

public record HTTPResponse<T>(
        int status,
        T body,
        McpErrorResponse error,
        Map<String, String> headers
) {

    public HTTPResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("HTTP response status must be between 100 and 599");
        }
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static <T> HTTPResponse<T> ok(T body) {
        return new HTTPResponse<>(200, body, null, Map.of());
    }

    public static <T> HTTPResponse<T> created(T body) {
        return new HTTPResponse<>(201, body, null, Map.of());
    }

    public static HTTPResponse<Void> noContent() {
        return new HTTPResponse<>(204, null, null, Map.of());
    }

    public static <T> HTTPResponse<T> error(int status, McpErrorResponse error) {
        return new HTTPResponse<>(status, null, error, Map.of());
    }
}
