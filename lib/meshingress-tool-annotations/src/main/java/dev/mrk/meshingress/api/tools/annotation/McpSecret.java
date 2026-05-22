package dev.mrk.meshingress.api.tools.annotation;

import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface McpSecret {

    @Pattern("[A-Za-z][A-Za-z0-9._-]*")
    String name();

    @Pattern("[A-Za-z0-9._:/-]+")
    String ref();
}
