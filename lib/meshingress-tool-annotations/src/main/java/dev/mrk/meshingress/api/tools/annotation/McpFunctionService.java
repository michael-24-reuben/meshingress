package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requests a typed provider for a callable MCP function parameter.
 *
 * <p>This parameter is resolved internally from {@code McpCallContext.services()}.
 * The resolved provider declares any client initialization input itself. That
 * input is published separately in this function's requirement metadata and is
 * submitted by clients under {@code tools/call.params.requirements}.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionService {
    /** Optional stable name for this function-level service requirement. */
    String value() default "";

    /** Human-readable label for this function-level service requirement. */
    String title() default "";

    /** Human-readable explanation of why this function requires the capability. */
    String description() default "";

    /**
     * Capability contract requested by the callable function. When omitted,
     * the function parameter's declared Java type is used.
     */
    Class<?> type() default Void.class;

    /** Optional concrete provider choices when more than one provider matches {@link #type()}. */
    Class<?>[] oneOf() default {};

}
