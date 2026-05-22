package dev.mrk.meshingress.route.framework;

import java.time.Instant;
import java.util.Map;

public record McpRouteLifecycleEvent(
        String name,
        String routeId,
        long durationNanos,
        Map<String, Object> attributes
) {

    public McpRouteLifecycleEvent {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    static McpRouteLifecycleEvent started(String routeId, Instant startedAt) {
        return new McpRouteLifecycleEvent("route.execution.started", routeId, 0L, Map.of());
    }

    static McpRouteLifecycleEvent completed(String routeId, Instant startedAt, int status) {
        return new McpRouteLifecycleEvent(
                "route.execution.completed",
                routeId,
                McpRouteExecutionPipeline.durationNanosSince(startedAt),
                Map.of("status", status)
        );
    }

    static McpRouteLifecycleEvent rejected(String routeId, Instant startedAt, String reason) {
        return new McpRouteLifecycleEvent(
                "route.execution.rejected",
                routeId,
                McpRouteExecutionPipeline.durationNanosSince(startedAt),
                Map.of("reason", reason == null ? "" : reason)
        );
    }

    static McpRouteLifecycleEvent failed(String routeId, Instant startedAt, Throwable throwable) {
        return new McpRouteLifecycleEvent(
                "route.execution.failed",
                routeId,
                McpRouteExecutionPipeline.durationNanosSince(startedAt),
                Map.of("exceptionType", throwable.getClass().getName())
        );
    }
}
