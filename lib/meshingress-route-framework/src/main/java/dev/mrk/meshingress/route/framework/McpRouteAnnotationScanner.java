package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.annotations.McpRoute;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class McpRouteAnnotationScanner {

    public List<AnnotatedMcpRoute> scan(Collection<Class<?>> controllerTypes) {
        List<AnnotatedMcpRoute> routes = new ArrayList<>();
        for (Class<?> controllerType : controllerTypes) {
            routes.addAll(scan(controllerType));
        }
        return List.copyOf(routes);
    }

    public List<AnnotatedMcpRoute> scan(Class<?> controllerType) {
        List<AnnotatedMcpRoute> routes = new ArrayList<>();
        for (Method method : controllerType.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(McpRoute.class)) {
                continue;
            }
            routes.add(AnnotatedMcpRoute.from(controllerType, method));
        }
        return List.copyOf(routes);
    }
}
