package dev.mrk.meshingress.route.framework;

import org.slf4j.Logger;

public class StructuredLoggingRouteExecutionObserver implements McpRouteExecutionObserver {

    private final Logger logger;

    public StructuredLoggingRouteExecutionObserver(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onEvent(McpRouteLifecycleEvent event) {
        logger.info(
                "event={} routeId={} durationNanos={} attributes={}",
                event.name(),
                event.routeId(),
                event.durationNanos(),
                event.attributes()
        );
    }
}
