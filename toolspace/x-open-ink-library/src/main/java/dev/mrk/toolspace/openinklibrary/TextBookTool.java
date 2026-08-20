package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@McpTool(
        value = "text",
        title = "Open Ink Library: Text Books",
        description = "Discover installed novel and serialized-prose source adapters."
)
@McpToolScopes({McpToolScope.HTTP_CLIENT, McpToolScope.EXTERNAL_API_READ})
public final class TextBookTool {
    private final BookSourceRegistry<TextBookSource> sources;
    private final ObjectMapper objectMapper;

    public TextBookTool(BookSourceRegistry<TextBookSource> sources, ObjectMapper objectMapper) {
        this.sources = Objects.requireNonNull(sources, "sources must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @McpConfigureMapping(timeoutMs = 20_000, audit = true)
    @McpFunction(value = "sources", title = "List text-book sources", description = "List installed text-book source adapters and capabilities.")
    public DispatchExecutionResult sources(McpCallContext context) {
        JsonNode payload = objectMapper.valueToTree(sources.list());
        return DispatchExecutionResult.builder()
                .array(payload)
                .structuredContent(payload)
                .status("completed")
                .summary("Listed installed text-book sources.")
                .build();
    }
}
