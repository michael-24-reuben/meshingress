package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.annotations.McpRoute;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class McpRouteAnnotationScanner {

    private final DefaultAvailabilityPolicyRegistry availabilityPolicyRegistry;

    public McpRouteAnnotationScanner() {
        this(new DefaultAvailabilityPolicyRegistry());
    }

    public McpRouteAnnotationScanner(DefaultAvailabilityPolicyRegistry availabilityPolicyRegistry) {
        this.availabilityPolicyRegistry = availabilityPolicyRegistry;
    }

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
            routes.add(AnnotatedMcpRoute.from(controllerType, method, availabilityAnnotations(method)));
        }
        return List.copyOf(routes);
    }

    private List<Annotation> availabilityAnnotations(Method method) {
        List<Annotation> annotations = new ArrayList<>();
        for (Annotation annotation : method.getAnnotations()) {
            if (availabilityPolicyRegistry.isAvailabilityAnnotation(annotation.annotationType())) {
                annotations.add(annotation);
            }
        }
        return List.copyOf(annotations);
    }
}
