package dev.mrk.meshingress.route.annotations;

import dev.mrk.meshingress.route.api.McpMiddleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpRequestMiddleware {

    Class<? extends McpMiddleware<?, ?, ?>>[] value();
}
