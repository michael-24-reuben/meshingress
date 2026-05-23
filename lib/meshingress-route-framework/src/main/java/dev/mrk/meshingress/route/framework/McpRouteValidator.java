package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.annotations.McpSecret;
import dev.mrk.meshingress.route.api.McpRouteValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class McpRouteValidator {

    public void validateOrThrow(List<AnnotatedMcpRoute> routes) {
        List<String> violations = validate(routes);
        if (!violations.isEmpty()) {
            throw new McpRouteValidationException(violations);
        }
    }

    public List<String> validate(List<AnnotatedMcpRoute> routes) {
        List<String> violations = new ArrayList<>();
        Set<String> routeIds = new HashSet<>();
        for (AnnotatedMcpRoute route : routes) {
            validateRoute(route, routeIds, violations);
        }
        return List.copyOf(violations);
    }

    private void validateRoute(AnnotatedMcpRoute route, Set<String> routeIds, List<String> violations) {
        String id = route.route().id();
        if (id == null || id.isBlank()) {
            violations.add(location(route) + " has blank route ID");
        } else if (!routeIds.add(id)) {
            violations.add("Duplicate MCP route ID: " + id);
        }
        if (route.route().path() == null || !route.route().path().startsWith("/")) {
            violations.add(location(route) + " route path must start with /");
        }
        validateConfiguration(route, violations);
    }

    private void validateConfiguration(AnnotatedMcpRoute route, List<String> violations) {
        if (route.configuration() == null) {
            return;
        }
        if (route.configuration().timeoutMs() < 0) {
            violations.add(location(route) + " timeoutMs must be >= 0");
        }
        for (McpSecret secret : route.configuration().secrets()) {
            if (secret.name().isBlank()) {
                violations.add(location(route) + " has a blank secret name");
            }
            if (secret.ref().isBlank()) {
                violations.add(location(route) + " has a blank secret ref");
            }
        }
    }

    private String location(AnnotatedMcpRoute route) {
        return route.controllerType().getName() + "#" + route.handlerMethod().getName();
    }
}
