package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionAvailabilityPolicy {

    Class<? extends McpAvailabilityPolicy<? extends Annotation>> value();
}

