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
     * Defines a unique tool ID, used to identify the tool in the system. Provides a failsafe
     * for when {@link #invocationName()} is not specified, as the tool can still be invoked using the tool ID with version.
     */
    @Pattern("[a-z][a-z0-9]*(\\.[a-z0-9]+)*")
    String value() default "";

    String title() default "";

    String description() default "";

    int version() default 1;

    boolean enabled() default true;

    ToolVisibility visibility() default ToolVisibility.PUBLIC;

    /**
     * The invocation name is used for invoking the tool without specifying the version.
     * If not specified, the invocation name will be the same as the tool ID without the version.
     * The invocation name must be in the format: `[a-z][a-z0-9]*(\.[a-z0-9]+)*`
     * Accepted formats
     * <pre>
     * • mytool
     * • mytool.subtool
     * </pre>
     */
    @Pattern("[a-z][a-z0-9]*(\\.[a-z0-9]+)*")
    String invocationName() default "";

    String defaultFunction() default "main";

    String handlerKey() default "";

    boolean dynamic() default false;
}
