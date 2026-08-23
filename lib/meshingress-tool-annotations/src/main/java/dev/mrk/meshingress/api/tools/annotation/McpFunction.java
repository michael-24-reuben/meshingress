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

    /**
     * Payload types that may appear under {@code structuredContent.data}.
     * <p>
     * The annotation scanner compiles these types into the function's MCP {@code outputSchema}.
     */
    Class<?>[] outputTypes() default {};

    /**
     * Optional explicit structured-output signal for consumers that need to override the output-schema fallback.
     */
    StructuredOutput structuredOutput() default StructuredOutput.INFER;

    ToolVisibility visibility() default ToolVisibility.PUBLIC;

    /*boolean destructive() default false;*/

    boolean enabled() default true;

    enum StructuredOutput {
        INFER,
        ENABLED,
        DISABLED
    }
}
