package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares JSON Schema constraints for an integral MCP input value. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpIntegerConstraints {

    long minimum() default Long.MIN_VALUE;

    long maximum() default Long.MAX_VALUE;

    long multipleOf() default 0;

    long defaultValue() default Long.MIN_VALUE;
}
