package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheKeyMode;
import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheStorage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// todo: implement
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpCacheResult {

    boolean enabled() default true;

    long ttlMs() default 0L;

    String namespace() default "";

    String keyPrefix() default "";

    String[] includeArguments() default {};

    String[] excludeArguments() default {};

    boolean includeToolId() default true;

    boolean includeFunctionName() default true;

    boolean includePrincipal() default false;

    boolean includeSession() default false;

    boolean cacheErrors() default false;

    boolean cacheEmptyResults() default true;

    McpCacheKeyMode keyMode() default McpCacheKeyMode.CANONICAL_ARGUMENTS;

    McpCacheStorage storage() default McpCacheStorage.DEFAULT;
}