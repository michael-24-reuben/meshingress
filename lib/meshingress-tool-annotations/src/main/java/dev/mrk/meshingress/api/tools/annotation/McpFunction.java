package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.ToolVisibility;
import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunction {
    String SEGMENT_NAME_REGEX = "[a-z][a-z0-9_-]*";

    /** @deprecated Use {@link #SEGMENT_NAME_REGEX}; function values are single segments. */
    @Deprecated
    String QUALIFIED_TOOL_FUNCTION_NAME_REGEX = SEGMENT_NAME_REGEX;

    @Pattern(SEGMENT_NAME_REGEX)
    String value() default "";

    String title() default "";

    String description() default "";

    ToolVisibility visibility() default ToolVisibility.PUBLIC;

    /*boolean destructive() default false;*/

    boolean enabled() default true;
}
