package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.tools.annotation.McpCacheResult;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class McpCachePolicyResolver {

    private final MeshingressProperties properties;

    public McpCachePolicyResolver(MeshingressProperties properties) {
        this.properties = properties;
    }

    public McpCachePolicy resolve(McpCacheResult annotation, String toolId) {
        Duration ttl = annotation.ttlMs() > 0
                ? Duration.ofMillis(annotation.ttlMs())
                : properties.cache().defaultTtl();
        Duration maxTtl = properties.cache().maxTtl();
        if (!maxTtl.isZero() && !maxTtl.isNegative() && ttl.compareTo(maxTtl) > 0) {
            ttl = maxTtl;
        }

        String namespace = annotation.namespace().isBlank() ? toolId : annotation.namespace();
        return new McpCachePolicy(
                properties.cache().enabled() && annotation.enabled() && !ttl.isZero() && !ttl.isNegative(),
                ttl,
                namespace,
                annotation.keyPrefix(),
                Arrays.stream(annotation.includeArguments()).filter(value -> !value.isBlank()).toList(),
                Arrays.stream(annotation.excludeArguments()).filter(value -> !value.isBlank()).toList(),
                annotation.includeToolId(),
                annotation.includeFunctionName(),
                annotation.includePrincipal(),
                annotation.includeSession(),
                annotation.cacheErrors(),
                annotation.cacheEmptyResults(),
                annotation.keyMode(),
                annotation.storage()
        );
    }
}
/*
*
architect/active/2026-05-25-mcp-cache-result-storage-design/notes.md
lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/cache/McpCacheStorage.java
lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java
app/meshingress-server/src/main/resources/application.properties
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheKey.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCachedValue.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheStore.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/NoOpMcpCacheStore.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/InMemoryMcpCacheStore.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/FileSystemMcpCacheStore.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCachePolicy.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCachePolicyResolver.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheStoreResolver.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheKeyGenerator.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/DispatchExecutionResultJsonCodec.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheManager.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandler.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandlerProvider.java
app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheManagerTests.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/FileSystemMcpCacheStore.java
pom.xml
app/meshingress-server/pom.xml
toolspace/powershell-cli-tool/pom.xml
architect/active/2026-05-25-mcp-cache-result-storage-design/todo.md
architect/active/2026-05-25-mcp-cache-result-storage-design/notes.md
architect/active/2026-05-25-mcp-cache-result-storage-design/assessment.md
architect/active/2026-05-25-mcp-cache-result-storage-design/fixes.md

*/