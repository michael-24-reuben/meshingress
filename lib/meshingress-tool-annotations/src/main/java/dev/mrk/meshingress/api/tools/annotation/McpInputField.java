package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpInputField {

    String value() default "";

    /** Optional human-readable JSON Schema title for this input. */
    String title() default "";

    String description() default "";

    boolean required() default true;

    /** Optional JSON Schema type override for values such as an explicit JSON null marker. */
    String schemaType() default "";

    /** Optional JSON Schema format used by clients to select a suitable value presentation. */
    String format() default "";
}
