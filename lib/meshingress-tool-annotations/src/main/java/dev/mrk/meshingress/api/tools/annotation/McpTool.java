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
     * A regular expression that matches a valid MCP tool name.
     * The format is: {@code [a-z][a-z0-9_-]*(?:\.[a-z][a-z0-9_-]*)*}
     * <p>
     * Enforces matching pattern for foreign keys in {@link McpTool#value()}
     */
    String QUALIFIED_TOOL_NAME_REGEX = "[a-z][a-z0-9_-]*(?:\\.[a-z][a-z0-9_-]*)*";

    /**
     * Defines a unique tool ID, used to identify the tool in the system.
     */
    @Pattern(QUALIFIED_TOOL_NAME_REGEX)
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
