package dev.mrk.meshingress.route.annotations;

import dev.mrk.meshingress.route.api.AvailabilityPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpAvailabilityPolicy {

    Class<? extends AvailabilityPolicy<?>> value();
}
