package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Annotation;

/**
 * Associates an availability annotation with its definition-time validator.
 * <p>
 * The condition checks whether the annotation is configured correctly on a tool method, such as whether a
 * required flag name is present or a time range is well formed. It does not decide whether the tool may run
 * for a particular request; that decision belongs to {@link McpFunctionAvailabilityPolicy}.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionAvailabilityCondition {

    /**
     * Validator for the annotated availability annotation's configuration.
     */
    Class<? extends McpAvailabilityCondition<? extends Annotation>> value();
}

