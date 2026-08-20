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
     * A regular expression that matches one valid MCP name segment.
     * Dots belong exclusively to the generated public identifier delimiters.
     */
    String SEGMENT_NAME_REGEX = "[a-z][a-z0-9_-]*";

    /** @deprecated Use {@link #SEGMENT_NAME_REGEX}; tool values are single segments. */
    @Deprecated
    String QUALIFIED_TOOL_NAME_REGEX = SEGMENT_NAME_REGEX;

    /**
     * Defines the module-local tool-family segment. The manifest namespace and
     * {@link McpFunction} value complete the public identifier.
     */
    @Pattern(SEGMENT_NAME_REGEX)
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
