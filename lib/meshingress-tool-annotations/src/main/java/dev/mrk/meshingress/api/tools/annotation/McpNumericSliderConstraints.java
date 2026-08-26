package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requests the Studio slider control for a bounded integral or decimal input.
 *
 * <p>The companion numeric constraint must provide both bounds. Decimal sliders
 * must also provide {@code multipleOf} so each slider position is a valid value.</p>
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpNumericSliderConstraints {
}
