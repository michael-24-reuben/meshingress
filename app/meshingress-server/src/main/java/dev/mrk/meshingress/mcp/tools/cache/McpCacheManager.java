package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpCacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.function.Supplier;

@Component
public class McpCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpCacheManager.class);

    private final McpCachePolicyResolver policyResolver;
    private final McpCacheKeyGenerator keyGenerator;
    private final McpCacheStoreResolver storeResolver;
    private final DispatchExecutionResultJsonCodec resultCodec;

    public McpCacheManager(
            McpCachePolicyResolver policyResolver,
            McpCacheKeyGenerator keyGenerator,
            McpCacheStoreResolver storeResolver,
            DispatchExecutionResultJsonCodec resultCodec
    ) {
        this.policyResolver = policyResolver;
        this.keyGenerator = keyGenerator;
        this.storeResolver = storeResolver;
        this.resultCodec = resultCodec;
    }

    public DispatchExecutionResult execute(
            McpCacheResult annotation,
            String toolId,
            String functionName,
            ObjectNode arguments,
            McpCallContext context,
            Supplier<DispatchExecutionResult> invocation
    ) {
        if (annotation == null) {
            return invocation.get();
        }

        McpCachePolicy policy = policyResolver.resolve(annotation, toolId);
        if (!policy.enabled()) {
            return invocation.get();
        }

        McpCacheKey key = keyGenerator.generate(policy, toolId, functionName, arguments, context);
        McpCacheStore store = storeResolver.resolve(policy);
        Instant now = Instant.now();

        try {
            var cached = store.get(key);
            if (cached.isPresent()) {
                McpCachedValue value = cached.get();
                if (!value.isExpired(now)) {
                    return resultCodec.fromJson(value.result());
                }
                store.delete(key);
            }
        } catch (Exception exception) {
            LOGGER.debug("MCP cache read failed; executing tool function: key={}", key, exception);
        }

        DispatchExecutionResult result = invocation.get();
        if (!isCacheable(policy, result)) {
            return result;
        }

        Instant createdAt = Instant.now();
        McpCachedValue value = new McpCachedValue(
                createdAt,
                createdAt.plus(policy.ttl()),
                resultCodec.toJson(result)
        );
        try {
            store.put(key, value);
        } catch (Exception exception) {
            LOGGER.debug("MCP cache write failed; returning uncached result: key={}", key, exception);
        }
        return result;
    }

    private boolean isCacheable(McpCachePolicy policy, DispatchExecutionResult result) {
        if (result.isError() && !policy.cacheErrors()) {
            return false;
        }
        boolean empty = result.content().isEmpty() && result.structuredContent().isEmpty();
        return !empty || policy.cacheEmptyResults();
    }
}
