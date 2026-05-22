package dev.mrk.meshingress.route.framework;

public interface McpRouteExecutionObserver {

    McpRouteExecutionObserver NOOP = event -> {
    };

    void onEvent(McpRouteLifecycleEvent event);
}
