package dev.mrk.meshingress.mcp.tools.cache;

import java.util.Optional;

public interface McpCacheStore {

    Optional<McpCachedValue> get(McpCacheKey key);

    void put(McpCacheKey key, McpCachedValue value);

    void delete(McpCacheKey key);

    default void cleanupExpired() {
    }
}
