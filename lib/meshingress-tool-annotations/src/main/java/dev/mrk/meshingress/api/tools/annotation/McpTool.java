package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.ToolVisibility;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

    String value() default "";

    String title() default "";

    String description() default "";

    int version() default 1;

    boolean enabled() default true;

    ToolVisibility visibility() default ToolVisibility.PUBLIC;

    String invocationName() default "";

    String defaultFunction() default "main";

    String handlerKey() default "";

    boolean destructive() default false;

    boolean dynamic() default false;
}
