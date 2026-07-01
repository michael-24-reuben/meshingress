package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpCacheResult;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheStorage;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.annotation.AnnotatedMcpToolHandler;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpCacheManagerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void annotatedFunctionReturnsCachedResultForRepeatedCalls() {
        CounterTool tool = new CounterTool();
        AnnotatedMcpToolHandler handler = handler(tool, "file");

        ObjectNode first = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);
        ObjectNode second = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);

        assertEquals(1, first.path("structuredContent").path("count").asInt());
        assertEquals(1, second.path("structuredContent").path("count").asInt());
        assertEquals(1, tool.calls);
    }

    @Test
    void expiredEntriesAreMisses() throws Exception {
        ExpiringTool tool = new ExpiringTool();
        AnnotatedMcpToolHandler handler = handler(tool, "memory");

        ObjectNode first = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);
        Thread.sleep(20);
        ObjectNode second = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);

        assertEquals(1, first.path("structuredContent").path("count").asInt());
        assertEquals(2, second.path("structuredContent").path("count").asInt());
    }

    @Test
    void excludedArgumentsDoNotChangeCacheKey() {
        ExcludeArgumentTool tool = new ExcludeArgumentTool();
        AnnotatedMcpToolHandler handler = handler(tool, "memory");

        ObjectNode first = call(handler, args("value", "alpha", "ignored", "one"), context("Bearer one", "session-a")).toJson(objectMapper);
        ObjectNode second = call(handler, args("value", "alpha", "ignored", "two"), context("Bearer one", "session-a")).toJson(objectMapper);

        assertEquals(1, first.path("structuredContent").path("count").asInt());
        assertEquals(1, second.path("structuredContent").path("count").asInt());
        assertEquals(1, tool.calls);
    }

    @Test
    void principalCanIsolateCacheKeys() {
        PrincipalTool tool = new PrincipalTool();
        AnnotatedMcpToolHandler handler = handler(tool, "memory");

        ObjectNode first = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);
        ObjectNode second = call(handler, args("value", "alpha"), context("Bearer two", "session-a")).toJson(objectMapper);
        ObjectNode third = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);

        assertEquals(1, first.path("structuredContent").path("count").asInt());
        assertEquals(2, second.path("structuredContent").path("count").asInt());
        assertEquals(1, third.path("structuredContent").path("count").asInt());
    }

    @Test
    void errorAndEmptyResultsFollowCacheabilityFlags() {
        ErrorTool errorTool = new ErrorTool();
        EmptyTool emptyTool = new EmptyTool();

        AnnotatedMcpToolHandler errorHandler = handler(errorTool, "memory");
        AnnotatedMcpToolHandler emptyHandler = handler(emptyTool, "memory");

        call(errorHandler, objectMapper.createObjectNode(), context("Bearer one", "session-a"));
        call(errorHandler, objectMapper.createObjectNode(), context("Bearer one", "session-a"));
        call(emptyHandler, objectMapper.createObjectNode(), context("Bearer one", "session-a"));
        call(emptyHandler, objectMapper.createObjectNode(), context("Bearer one", "session-a"));

        assertEquals(2, errorTool.calls);
        assertEquals(2, emptyTool.calls);
    }

    @Test
    void corruptFilesystemEntryIsTreatedAsMiss() throws Exception {
        CounterTool tool = new CounterTool();
        AnnotatedMcpToolHandler handler = handler(tool, "file");

        call(handler, args("value", "alpha"), context("Bearer one", "session-a"));
        Path cacheFile = Files.walk(tempDir)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .findFirst()
                .orElseThrow();
        Files.writeString(cacheFile, "{not-json");

        ObjectNode second = call(handler, args("value", "alpha"), context("Bearer one", "session-a")).toJson(objectMapper);

        assertEquals(2, second.path("structuredContent").path("count").asInt());
    }

    private DispatchExecutionResult call(AnnotatedMcpToolHandler handler, ObjectNode args, McpCallContext context) {
        return handler.call(args, context);
    }

    private AnnotatedMcpToolHandler handler(Object toolBean, String defaultStorage) {
        MeshingressProperties properties = properties(defaultStorage);
        McpCacheManager manager = new McpCacheManager(
                new McpCachePolicyResolver(properties),
                new McpCacheKeyGenerator(objectMapper),
                new McpCacheStoreResolver(
                        properties,
                        new NoOpMcpCacheStore(),
                        new InMemoryMcpCacheStore(),
                        new FileSystemMcpCacheStore(objectMapper, properties)
                ),
                new DispatchExecutionResultJsonCodec(objectMapper)
        );
        AnnotatedMcpTool tool = new McpToolAnnotationScanner(objectMapper).scan(toolBean.getClass());
        return new AnnotatedMcpToolHandler(
                toolBean,
                tool,
                tool.functions().getFirst(),
                objectMapper,
                new TypedJsonArgumentBinder(),
                manager
        );
    }

    private MeshingressProperties properties(String defaultStorage) {
        return new MeshingressProperties(
                null,
                null,
                null,
                null,
                new MeshingressProperties.Cache(
                        true,
                        tempDir.toString(),
                        defaultStorage,
                        tempDir.toString(),
                        Duration.ofMinutes(5),
                        Duration.ofHours(1),
                        DataSize.ofMegabytes(1),
                        DataSize.ofMegabytes(256),
                        true,
                        true
                ),
                null,
                null,
                null,
                null,
                null
        );
    }

    private ObjectNode args(String... pairs) {
        ObjectNode node = objectMapper.createObjectNode();
        for (int index = 0; index < pairs.length; index += 2) {
            node.put(pairs[index], pairs[index + 1]);
        }
        return node;
    }

    private McpCallContext context(String authorization, String sessionId) {
        return new McpCallContext(authorization, "admin", sessionId, "request-id");
    }

    @McpTool("cache.counter")
    static class CounterTool {
        int calls;

        @McpFunction("count")
        @McpCacheResult(ttlMs = 60_000, namespace = "counter", storage = McpCacheStorage.DEFAULT)
        Map<String, Object> count(@McpFunctionParam("value") String value) {
            return Map.of("count", ++calls, "value", value);
        }
    }

    @McpTool("cache.expiring")
    static class ExpiringTool {
        int calls;

        @McpFunction("count")
        @McpCacheResult(ttlMs = 1, namespace = "expiring", storage = McpCacheStorage.MEMORY)
        Map<String, Object> count(@McpFunctionParam("value") String value) {
            return Map.of("count", ++calls, "value", value);
        }
    }

    @McpTool("cache.exclude")
    static class ExcludeArgumentTool {
        int calls;

        @McpFunction("count")
        @McpCacheResult(ttlMs = 60_000, namespace = "exclude", excludeArguments = "ignored", storage = McpCacheStorage.MEMORY)
        Map<String, Object> count(
                @McpFunctionParam("value") String value,
                @McpFunctionParam("ignored") String ignored
        ) {
            return Map.of("count", ++calls, "value", value, "ignored", ignored);
        }
    }

    @McpTool("cache.principal")
    static class PrincipalTool {
        int calls;

        @McpFunction("count")
        @McpCacheResult(ttlMs = 60_000, namespace = "principal", includePrincipal = true, storage = McpCacheStorage.MEMORY)
        Map<String, Object> count(@McpFunctionParam("value") String value) {
            return Map.of("count", ++calls, "value", value);
        }
    }

    @McpTool("cache.error")
    static class ErrorTool {
        int calls;

        @McpFunction("error")
        @McpCacheResult(ttlMs = 60_000, namespace = "error", cacheErrors = false, storage = McpCacheStorage.MEMORY)
        DispatchExecutionResult error() {
            calls++;
            return DispatchExecutionResult.builder().text("failed").error(true).build();
        }
    }

    @McpTool("cache.empty")
    static class EmptyTool {
        int calls;

        @McpFunction("empty")
        @McpCacheResult(ttlMs = 60_000, namespace = "empty", cacheEmptyResults = false, storage = McpCacheStorage.MEMORY)
        DispatchExecutionResult empty() {
            calls++;
            return DispatchExecutionResult.builder().build();
        }
    }
}
