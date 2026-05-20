package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.ToolVisibility;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunction {

    String value() default "";

    String title() default "";

    String description() default "";

    ToolVisibility visibility() default ToolVisibility.PUBLIC;

    boolean enabled() default true;
}
