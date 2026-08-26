package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares safe length bounds for a secret without publishing a default value. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpSecretConstraints {

    int minLength() default -1;

    int maxLength() default -1;
}
