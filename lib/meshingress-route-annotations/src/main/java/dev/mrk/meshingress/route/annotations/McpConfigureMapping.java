package dev.mrk.meshingress.route.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpConfigureMapping {

    McpSecret[] secrets() default {};

    boolean audit() default false;

    boolean debugTrace() default false;

    long timeoutMs() default 0L;

    McpRouteStability stability() default McpRouteStability.STABLE;
}
