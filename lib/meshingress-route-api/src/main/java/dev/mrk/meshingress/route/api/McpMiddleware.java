package dev.mrk.meshingress.route.api;

public interface McpMiddleware<Q, P, B> {

    HTTPRequest<Q, P, B> apply(
            HTTPRequest<Q, P, B> request,
            McpRouteExecutionContext context
    );
}
