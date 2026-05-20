package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.schema.McpJsonSchemaProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpInputSchema {

    String description() default "";

    Class<? extends McpJsonSchemaProvider> provider() default McpJsonSchemaProvider.class;
}
