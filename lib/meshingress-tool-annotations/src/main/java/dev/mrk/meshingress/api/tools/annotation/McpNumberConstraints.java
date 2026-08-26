package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares JSON Schema constraints for a non-integral numeric MCP input value. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpNumberConstraints {

    double minimum() default Double.NaN;

    double maximum() default Double.NaN;

    double multipleOf() default Double.NaN;

    double defaultValue() default Double.NaN;
}
