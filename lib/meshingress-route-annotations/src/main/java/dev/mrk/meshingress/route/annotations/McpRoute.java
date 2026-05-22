package dev.mrk.meshingress.route.annotations;

import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpRoute {

    @Pattern("[a-z][a-z0-9]*(\\.[a-z0-9]+)*\\.v[0-9]+")
    String id();

    McpHttpMethod method();

    @Pattern("/.*")
    String path();
}
