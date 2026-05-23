package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.HTTPResponse;
import dev.mrk.meshingress.route.api.McpMiddleware;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class McpRouteExecutionPipeline {

    private final McpMiddlewareExecutor middlewareExecutor;
    private final StandardMcpRouteErrorMapper errorMapper;
    private final McpRouteExecutionObserver observer;

    public McpRouteExecutionPipeline() {
        this(
                new McpMiddlewareExecutor(),
                new StandardMcpRouteErrorMapper(),
                McpRouteExecutionObserver.NOOP
        );
    }

    public McpRouteExecutionPipeline(
            McpMiddlewareExecutor middlewareExecutor,
            StandardMcpRouteErrorMapper errorMapper,
            McpRouteExecutionObserver observer
    ) {
        this.middlewareExecutor = middlewareExecutor;
        this.errorMapper = errorMapper;
        this.observer = observer == null ? McpRouteExecutionObserver.NOOP : observer;
    }

    public <Q, P, B, T> HTTPResponse<T> execute(
            AnnotatedMcpRoute route,
            HTTPRequest<Q, P, B> request,
            McpRouteExecutionContext context,
            List<? extends McpMiddleware<Q, P, B>> middleware,
            McpRouteInvoker<Q, P, B, T> invoker
    ) {
        Instant startedAt = Instant.now();
        observer.onEvent(McpRouteLifecycleEvent.started(route.routeId(), startedAt));
        try {
            HTTPRequest<Q, P, B> normalizedRequest = request.withRouteId(route.routeId());
            HTTPRequest<Q, P, B> guardedRequest = middlewareExecutor.execute(normalizedRequest, context, middleware);
            HTTPResponse<T> response = invoker.invoke(guardedRequest, context);
            observer.onEvent(McpRouteLifecycleEvent.completed(route.routeId(), startedAt, response.status()));
            return response;
        } catch (Throwable throwable) {
            observer.onEvent(McpRouteLifecycleEvent.failed(route.routeId(), startedAt, throwable));
            @SuppressWarnings("unchecked")
            HTTPResponse<T> mappedResponse = (HTTPResponse<T>) errorMapper.map(throwable);
            return mappedResponse;
        }
    }

    static long durationNanosSince(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toNanos();
    }
}
