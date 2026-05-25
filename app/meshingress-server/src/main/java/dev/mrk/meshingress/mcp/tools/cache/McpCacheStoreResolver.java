package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheStorage;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class McpCacheStoreResolver {

    private final MeshingressProperties properties;
    private final NoOpMcpCacheStore noOpStore;
    private final InMemoryMcpCacheStore memoryStore;
    private final FileSystemMcpCacheStore fileStore;

    public McpCacheStoreResolver(
            MeshingressProperties properties,
            NoOpMcpCacheStore noOpStore,
            InMemoryMcpCacheStore memoryStore,
            FileSystemMcpCacheStore fileStore
    ) {
        this.properties = properties;
        this.noOpStore = noOpStore;
        this.memoryStore = memoryStore;
        this.fileStore = fileStore;
    }

    public McpCacheStore resolve(McpCachePolicy policy) {
        McpCacheStorage storage = policy.storage() == McpCacheStorage.DEFAULT
                ? defaultStorage()
                : policy.storage();
        return switch (storage) {
            case NONE -> noOpStore;
            case MEMORY -> memoryStore;
            case FILE -> fileStore;
            case DEFAULT -> fileStore;
        };
    }

    private McpCacheStorage defaultStorage() {
        String value = properties.cache().defaultStorage().trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return McpCacheStorage.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return McpCacheStorage.FILE;
        }
    }
}
