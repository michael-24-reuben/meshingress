package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.api.McpRouteValidationException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class McpRouteRegistry {

    private final Map<String, AnnotatedMcpRoute> routesById = new LinkedHashMap<>();

    public McpRouteRegistry(Collection<AnnotatedMcpRoute> routes) {
        for (AnnotatedMcpRoute route : routes) {
            register(route);
        }
    }

    public Optional<AnnotatedMcpRoute> findById(String routeId) {
        return Optional.ofNullable(routesById.get(routeId));
    }

    public List<AnnotatedMcpRoute> routes() {
        return List.copyOf(routesById.values());
    }

    private void register(AnnotatedMcpRoute route) {
        AnnotatedMcpRoute previous = routesById.putIfAbsent(route.routeId(), route);
        if (previous != null) {
            throw new McpRouteValidationException(List.of("Duplicate MCP route ID: " + route.routeId()));
        }
    }
}
