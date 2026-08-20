package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;

import java.lang.annotation.*;

/**
 * Associates an availability annotation with its runtime availability policy.
 * <p>
 * The policy decides whether a correctly configured tool function may run in the current context, for example
 * based on a feature flag, the time, caller details, or future request arguments. Configuration validation
 * belongs to {@link McpFunctionAvailabilityCondition}.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionAvailabilityPolicy {

    /**
     * Policy that evaluates whether the annotated availability rule currently allows the function to run.
     */
    Class<? extends McpAvailabilityPolicy<? extends Annotation>> value();
}

