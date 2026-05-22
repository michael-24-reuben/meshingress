package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.annotations.McpSecret;
import dev.mrk.meshingress.route.annotations.availability.AvailabilityCondition;
import dev.mrk.meshingress.route.annotations.availability.AvailabilityValidationContext;
import dev.mrk.meshingress.route.annotations.availability.RouteAvailabilityCondition;
import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.HTTPResponse;
import dev.mrk.meshingress.route.api.McpRouteValidationException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class McpRouteValidator {

    private final ApplicationContext applicationContext;
    private final DefaultAvailabilityPolicyRegistry availabilityPolicyRegistry;

    public McpRouteValidator() {
        this(defaultApplicationContext(), new DefaultAvailabilityPolicyRegistry());
    }

    public McpRouteValidator(DefaultAvailabilityPolicyRegistry availabilityPolicyRegistry) {
        this(defaultApplicationContext(), availabilityPolicyRegistry);
    }

    public McpRouteValidator(ApplicationContext applicationContext) {
        this(applicationContext, new DefaultAvailabilityPolicyRegistry());
    }

    public McpRouteValidator(ApplicationContext applicationContext, DefaultAvailabilityPolicyRegistry availabilityPolicyRegistry) {
        this.applicationContext = applicationContext;
        this.availabilityPolicyRegistry = availabilityPolicyRegistry;
    }

    public void validateOrThrow(List<AnnotatedMcpRoute> routes) {
        List<String> violations = validate(routes);
        if (!violations.isEmpty()) {
            throw new McpRouteValidationException(violations);
        }
    }

    public List<String> validate(List<AnnotatedMcpRoute> routes) {
        List<String> violations = new ArrayList<>();
        Set<String> routeIds = new HashSet<>();
        for (AnnotatedMcpRoute route : routes) {
            validateRoute(route, routeIds, violations);
        }
        return List.copyOf(violations);
    }

    private void validateRoute(@NonNull AnnotatedMcpRoute route, Set<String> routeIds, List<String> violations) {
        String id = route.route().id();
        if (id == null || id.isBlank()) {
            violations.add(location(route) + " has blank route ID");
        } else if (!routeIds.add(id)) {
            violations.add("Duplicate MCP route ID: " + id);
        }
        if (route.route().path() == null || !route.route().path().startsWith("/")) {
            violations.add(location(route) + " route path must start with /");
        }
        validateHandlerSignature(route, violations);
        validateConfiguration(route, violations);
        validateAvailability(route, violations);
    }

    private void validateHandlerSignature(@NonNull AnnotatedMcpRoute route, List<String> violations) {
        Method method = route.handlerMethod();
        if (method.getParameterCount() != 1 || !HTTPRequest.class.isAssignableFrom(method.getParameterTypes()[0])) {
            violations.add(location(route) + " must accept exactly one HTTPRequest parameter");
        }
        if (!HTTPResponse.class.isAssignableFrom(method.getReturnType())) {
            violations.add(location(route) + " must return HTTPResponse");
        }
    }

    private void validateConfiguration(@NonNull AnnotatedMcpRoute route, List<String> violations) {
        if (route.configuration() == null) {
            return;
        }
        if (route.configuration().timeoutMs() < 0) {
            violations.add(location(route) + " timeoutMs must be >= 0");
        }
        for (McpSecret secret : route.configuration().secrets()) {
            if (secret.name().isBlank()) {
                violations.add(location(route) + " has a blank secret name");
            }
            if (secret.ref().isBlank()) {
                violations.add(location(route) + " has a blank secret ref");
            }
        }
    }

    private void validateAvailability(@NonNull AnnotatedMcpRoute route, List<String> violations) {
        AvailabilityValidationContext context = new AvailabilityValidationContext(route.controllerType(), route.handlerMethod());
        for (Annotation annotation : route.availabilityAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (availabilityPolicyRegistry.findPolicy(annotationType).isEmpty()) {
                violations.add(context.location() + " has no policy for availability annotation " + annotationType.getName());
                continue;
            }
            RouteAvailabilityCondition conditionDeclaration = annotationType.getAnnotation(RouteAvailabilityCondition.class);
            if (conditionDeclaration == null) {
                violations.add(context.location() + " availability annotation is missing @RouteAvailabilityCondition: " + annotationType.getName());
                continue;
            }
            validateWithCondition(conditionDeclaration.value(), annotation, context, violations);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation> void validateWithCondition(
            Class<? extends AvailabilityCondition<? extends Annotation>> conditionType,
            Annotation annotation,
            AvailabilityValidationContext context,
            List<String> violations
    ) {
        try {
            AvailabilityCondition<A> condition = (AvailabilityCondition<A>) applicationContext.getBean(conditionType);
            A typedAnnotation = (A) annotation;
            List<String> conditionViolations = condition.validate(typedAnnotation, context);
            if (conditionViolations != null) {
                violations.addAll(conditionViolations);
            }
        } catch (NoSuchBeanDefinitionException exception) {
            violations.add(context.location() + " availability condition is not registered as a Spring bean: " + conditionType.getName());
        } catch (RuntimeException exception) {
            violations.add(context.location() + " availability condition failed: " + conditionType.getName() + ": " + exception.getMessage());
        }
    }

    private String location(AnnotatedMcpRoute route) {
        return route.controllerType().getName() + "#" + route.handlerMethod().getName();
    }

    private static ApplicationContext defaultApplicationContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // No built-in condition beans registered by default - application should provide condition beans.
        context.refresh();
        return context;
    }
}
