package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheKeyMode;
import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheStorage;

import java.time.Duration;
import java.util.List;

public record McpCachePolicy(
        boolean enabled,
        Duration ttl,
        String namespace,
        String keyPrefix,
        List<String> includeArguments,
        List<String> excludeArguments,
        boolean includeToolId,
        boolean includeFunctionName,
        boolean includePrincipal,
        boolean includeSession,
        boolean cacheErrors,
        boolean cacheEmptyResults,
        McpCacheKeyMode keyMode,
        McpCacheStorage storage
) {
}
