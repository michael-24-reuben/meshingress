package dev.mrk.meshingress.route.api;

public class McpRouteFrameworkException extends RuntimeException {

    private final String code;

    public McpRouteFrameworkException(String code, String message) {
        super(message);
        this.code = code;
    }

    public McpRouteFrameworkException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
