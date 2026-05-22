package dev.mrk.meshingress.route.api;

import java.util.List;

public class McpRouteValidationException extends McpRouteFrameworkException {

    private final List<String> violations;

    public McpRouteValidationException(List<String> violations) {
        super("route.validation.failed", String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
