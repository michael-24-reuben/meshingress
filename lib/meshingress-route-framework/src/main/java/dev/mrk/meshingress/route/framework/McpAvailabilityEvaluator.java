package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.api.AvailabilityDecision;
import dev.mrk.meshingress.route.api.AvailabilityPolicy;
import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;

import dev.mrk.meshingress.api.tools.annotation.McpAvailabilityMode;

import java.lang.annotation.Annotation;

public class McpAvailabilityEvaluator {

    private final DefaultAvailabilityPolicyRegistry policyRegistry;

    public McpAvailabilityEvaluator() {
        this(new DefaultAvailabilityPolicyRegistry());
    }

    public McpAvailabilityEvaluator(DefaultAvailabilityPolicyRegistry policyRegistry) {
        this.policyRegistry = policyRegistry;
    }

    public AvailabilityDecision evaluate(
            AnnotatedMcpRoute route,
            HTTPRequest<?, ?, ?> request,
            McpRouteExecutionContext context
    ) {
        if (route.availabilityAnnotations().isEmpty()) {
            return AvailabilityDecision.allow();
        }
        McpAvailabilityMode mode = route.configuration() == null
                ? McpAvailabilityMode.ALL
                : route.configuration().availabilityMode();
        boolean anyAllowed = false;
        AvailabilityDecision firstDenied = null;
        for (Annotation annotation : route.availabilityAnnotations()) {
            AvailabilityDecision decision = evaluateAnnotation(annotation, request, context);
            if (decision.allowed()) {
                anyAllowed = true;
                if (mode == McpAvailabilityMode.ANY) {
                    return decision;
                }
            } else {
                if (firstDenied == null) {
                    firstDenied = decision;
                }
                if (mode == McpAvailabilityMode.ALL) {
                    return decision;
                }
            }
        }
        if (mode == McpAvailabilityMode.ANY && !anyAllowed) {
            return firstDenied == null ? AvailabilityDecision.deny("route availability denied") : firstDenied;
        }
        return AvailabilityDecision.allow();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AvailabilityDecision evaluateAnnotation(
            Annotation annotation,
            HTTPRequest<?, ?, ?> request,
            McpRouteExecutionContext context
    ) {
        AvailabilityPolicy policy = policyRegistry.findPolicy(annotation.annotationType())
                .orElseThrow(() -> new IllegalStateException("No availability policy for " + annotation.annotationType().getName()));
        return policy.evaluate(annotation, request, context);
    }
}
