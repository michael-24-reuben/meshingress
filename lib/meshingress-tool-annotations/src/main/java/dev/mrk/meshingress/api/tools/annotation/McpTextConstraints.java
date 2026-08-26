package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares JSON Schema constraints for a text input. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTextConstraints {

    int minLength() default -1;

    int maxLength() default -1;

    String pattern() default "";

    String defaultValue() default "";

    boolean hasDefault() default false;
}
