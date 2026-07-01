package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.ToolVisibility;
import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

    /**
     * Defines a unique tool ID, used to identify the tool in the system.
     */
    @Pattern("[a-z][a-z0-9-_]*(\\.[a-z][a-z0-9-_])*")
    String value();

    String title() default "";

    String description() default "";

    int version() default 1;

    boolean enabled() default true;

    ToolVisibility visibility() default ToolVisibility.PUBLIC;

    String defaultFunction() default "main";

    String handlerKey() default "";

    boolean dynamic() default false;
}
