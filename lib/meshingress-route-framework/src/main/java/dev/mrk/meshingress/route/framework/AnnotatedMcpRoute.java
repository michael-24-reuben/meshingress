package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.annotations.McpConfigureMapping;
import dev.mrk.meshingress.route.annotations.McpRequestMiddleware;
import dev.mrk.meshingress.route.annotations.McpRoute;
import dev.mrk.meshingress.route.api.McpMiddleware;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public record AnnotatedMcpRoute(
        Class<?> controllerType,
        Method handlerMethod,
        McpRoute route,
        McpConfigureMapping configuration,
        List<Class<? extends McpMiddleware<?, ?, ?>>> middlewareTypes,
        List<Annotation> availabilityAnnotations
) {

    public AnnotatedMcpRoute {
        middlewareTypes = middlewareTypes == null ? List.of() : List.copyOf(middlewareTypes);
        availabilityAnnotations = availabilityAnnotations == null ? List.of() : List.copyOf(availabilityAnnotations);
    }

    public String routeId() {
        return route.id();
    }

    public static AnnotatedMcpRoute from(
            Class<?> controllerType,
            Method handlerMethod,
            List<Annotation> availabilityAnnotations
    ) {
        McpRoute route = handlerMethod.getAnnotation(McpRoute.class);
        McpConfigureMapping configuration = handlerMethod.getAnnotation(McpConfigureMapping.class);
        McpRequestMiddleware middleware = handlerMethod.getAnnotation(McpRequestMiddleware.class);
        List<Class<? extends McpMiddleware<?, ?, ?>>> middlewareTypes = middleware == null
                ? List.of()
                : List.of(middleware.value());
        return new AnnotatedMcpRoute(
                controllerType,
                handlerMethod,
                route,
                configuration,
                middlewareTypes,
                availabilityAnnotations
        );
    }
}
