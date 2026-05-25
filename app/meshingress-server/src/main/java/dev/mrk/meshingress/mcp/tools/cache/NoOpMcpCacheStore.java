package dev.mrk.meshingress.mcp.tools.cache;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoOpMcpCacheStore implements McpCacheStore {

    @Override
    public Optional<McpCachedValue> get(McpCacheKey key) {
        return Optional.empty();
    }

    @Override
    public void put(McpCacheKey key, McpCachedValue value) {
    }

    @Override
    public void delete(McpCacheKey key) {
    }
}
