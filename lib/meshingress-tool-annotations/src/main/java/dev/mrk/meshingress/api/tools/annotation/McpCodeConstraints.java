package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares source-language and text bounds for a code input. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpCodeConstraints {

    String language() default "";

    int minLength() default -1;

    int maxLength() default -1;
}
