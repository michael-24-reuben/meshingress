package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.McpMiddleware;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;

import java.util.List;

public class McpMiddlewareExecutor {

    public <Q, P, B> HTTPRequest<Q, P, B> execute(
            HTTPRequest<Q, P, B> request,
            McpRouteExecutionContext context,
            List<? extends McpMiddleware<Q, P, B>> middleware
    ) {
        HTTPRequest<Q, P, B> current = request;
        for (McpMiddleware<Q, P, B> item : middleware) {
            current = item.apply(current, context);
            if (current == null) {
                throw new IllegalStateException("MCP route middleware must not return null: " + item.getClass().getName());
            }
        }
        return current;
    }
}
