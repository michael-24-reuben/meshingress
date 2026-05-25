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

    /**
     * Enables or disables result caching for the annotated function.
     */
    boolean enabled() default true;

    /**
     * Time-to-live for cached entries, in milliseconds.
     * A value of {@code 0} means the entry does not expire by annotation policy.
     */
    long ttlMs() default 0L;

    /**
     * Logical cache namespace used to group related cache entries.
     * When blank, the framework should derive a namespace from tool/function metadata.
     */
    String namespace() default "";

    /**
     * Optional prefix added to generated cache keys for grouping or readability.
     */
    String keyPrefix() default "";

    /**
     * Argument names to include when building the cache key.
     * When empty, all eligible arguments are considered unless excluded.
     */
    String[] includeArguments() default {};

    /**
     * Argument names to exclude when building the cache key.
     */
    String[] excludeArguments() default {};

    /**
     * Includes the tool identifier in the generated cache key.
     */
    boolean includeToolId() default true;

    /**
     * Includes the function name in the generated cache key.
     */
    boolean includeFunctionName() default true;

    /**
     * Includes the calling principal identity in the generated cache key.
     * Use this when cached results must be isolated per authenticated caller.
     */
    boolean includePrincipal() default false;

    /**
     * Includes the current session identifier in the generated cache key.
     * Use this for session-local cached results.
     */
    boolean includeSession() default false;

    /**
     * Allows error results to be cached.
     */
    boolean cacheErrors() default false;

    /**
     * Allows empty successful results to be cached.
     */
    boolean cacheEmptyResults() default true;

    /**
     * Defines how function arguments are converted into cache-key material.
     */
    McpCacheKeyMode keyMode() default McpCacheKeyMode.CANONICAL_ARGUMENTS;

    /**
     * Selects the cache storage backend for this function.
     * {@code DEFAULT} delegates to the configured application default.
     */
    McpCacheStorage storage() default McpCacheStorage.DEFAULT;
}