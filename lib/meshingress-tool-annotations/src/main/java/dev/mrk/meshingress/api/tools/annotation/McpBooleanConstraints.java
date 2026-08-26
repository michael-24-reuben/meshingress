package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares default metadata for a boolean input. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpBooleanConstraints {

    boolean defaultValue() default false;

    boolean hasDefault() default false;
}
