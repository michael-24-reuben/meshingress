package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.HTTPResponse;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;

@FunctionalInterface
public interface McpRouteInvoker<Q, P, B, T> {

    HTTPResponse<T> invoke(
            HTTPRequest<Q, P, B> request,
            McpRouteExecutionContext context
    ) throws Exception;
}
