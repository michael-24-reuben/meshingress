package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.scopes.McpToolScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolScopes {
    McpToolScope[] value() default {};
}