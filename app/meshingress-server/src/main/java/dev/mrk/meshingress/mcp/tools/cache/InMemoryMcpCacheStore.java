package dev.mrk.meshingress.mcp.tools.cache;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryMcpCacheStore implements McpCacheStore {

    private final ConcurrentMap<String, McpCachedValue> values = new ConcurrentHashMap<>();

    @Override
    public Optional<McpCachedValue> get(McpCacheKey key) {
        return Optional.ofNullable(values.get(mapKey(key)));
    }

    @Override
    public void put(McpCacheKey key, McpCachedValue value) {
        values.put(mapKey(key), value);
    }

    @Override
    public void delete(McpCacheKey key) {
        values.remove(mapKey(key));
    }

    @Override
    public void cleanupExpired() {
        Instant now = Instant.now();
        values.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String mapKey(McpCacheKey key) {
        return key.namespace() + ":" + key.hash();
    }
}
