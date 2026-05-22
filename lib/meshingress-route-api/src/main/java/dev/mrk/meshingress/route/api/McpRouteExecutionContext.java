package dev.mrk.meshingress.route.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record McpRouteExecutionContext(
        String routeId,
        String requestId,
        String correlationId,
        Object principal,
        String tenantId,
        Instant startedAt,
        Map<String, Object> attributes,
        List<String> auditTrace
) {

    public McpRouteExecutionContext {
        routeId = routeId == null ? "" : routeId;
        requestId = requestId == null ? "" : requestId;
        correlationId = correlationId == null ? "" : correlationId;
        tenantId = tenantId == null ? "" : tenantId;
        startedAt = startedAt == null ? Instant.now() : startedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        auditTrace = auditTrace == null ? List.of() : List.copyOf(auditTrace);
    }

    public static McpRouteExecutionContext forRoute(String routeId) {
        return new McpRouteExecutionContext(routeId, "", "", null, "", Instant.now(), Map.of(), List.of());
    }

    public McpRouteExecutionContext withAttribute(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Context attribute name must not be blank");
        }
        Map<String, Object> nextAttributes = new java.util.LinkedHashMap<>(attributes);
        nextAttributes.put(name, value);
        return new McpRouteExecutionContext(
                routeId,
                requestId,
                correlationId,
                principal,
                tenantId,
                startedAt,
                nextAttributes,
                auditTrace
        );
    }
}
