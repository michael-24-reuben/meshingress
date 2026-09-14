package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.ToolVisibility;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines a non-callable, internally resolved service provider class. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpService {
    /** Service identifier for a provider class. */
    String value() default "";

    Class<?>[] provides() default {};

    /**
     * Service requirements for a provider class.
     */
    McpServiceRequirement[] requires() default {};

    String title() default "";

    String description() default "";

    int version() default 1;

    boolean enabled() default true;

    ToolVisibility visibility() default ToolVisibility.PUBLIC;
}
