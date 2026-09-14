package dev.mrk.meshingress.api.services;

import dev.mrk.meshingress.api.McpCallContext;
import org.intellij.lang.annotations.Pattern;
import org.springframework.core.annotation.Order;

/**
 * Common lifecycle for an internal MCP service capability during one tool call.
 *
 * <p>A provider may declare a typed overload of {@code init(...)} on its concrete
 * class. The server scans that overload with the same parameter rules as a tool
 * function, publishes its input schema as the provider requirement schema, and
 * invokes it before the consuming tool function. This no-argument method remains
 * the optional lifecycle convention when the provider has no typed input.</p>
 */
public interface McpServiceLifecycle {

    /** Runs before the consuming tool function when no typed {@code init(...)} overload is declared. */
    default void init() {}

    /** Runs after the consuming tool function completes, including failures. */
    default void stop() {}
}
