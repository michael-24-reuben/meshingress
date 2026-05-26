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
