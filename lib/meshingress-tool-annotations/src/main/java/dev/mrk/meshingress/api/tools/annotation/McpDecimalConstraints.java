package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares arbitrary-precision decimal JSON Schema constraints. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpDecimalConstraints {

    String minimum() default "";

    String maximum() default "";

    String multipleOf() default "";

    String defaultValue() default "";
}
