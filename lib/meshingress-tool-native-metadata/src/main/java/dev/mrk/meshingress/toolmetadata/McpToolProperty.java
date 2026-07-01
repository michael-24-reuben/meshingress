package dev.mrk.meshingress.toolmetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(McpToolProperties.class)
public @interface McpToolProperty {
    String name();

    String description();

    String defaultValue() default "";

    String valueType() default "string";

    boolean required() default false;
}
