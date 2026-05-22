package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Annotation;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolAvailabilityCondition {

    Class<? extends AvailabilityCondition<? extends Annotation>> value();
}

