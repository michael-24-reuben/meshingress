package dev.mrk.toolspace.youtube;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.dispatch.data.RecordsContent;
import dev.mrk.toolspace.youtube.catalog.YoutubeCapabilityCatalog;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@McpTool(
        value = "catalog",
        title = "YouTube catalog",
        description = "Discover YouTube capabilities, providers, authorization requirements, and delivery status."
)
public final class YoutubeCatalogTool {
    private final ObjectMapper objectMapper;
    private final YoutubeCapabilityCatalog catalog;

    YoutubeCatalogTool(ObjectMapper objectMapper, YoutubeCapabilityCatalog catalog) {
        this.objectMapper = objectMapper;
        this.catalog = catalog;
    }

    @McpFunction(
            value = "capabilities",
            title = "List YouTube capabilities",
            description = "List implemented and planned YouTube function IDs, provider boundaries, authorization requirements, and delivery status."
    )
    public DispatchExecutionResult capabilities(McpCallContext context) {
        return records(catalog.capabilities(), "YouTube capability catalog returned.");
    }

    @McpFunction(
            value = "providers",
            title = "List YouTube providers",
            description = "List the provider boundary for official YouTube APIs, local media, and future transcript or AI providers."
    )
    public DispatchExecutionResult providers(McpCallContext context) {
        return records(catalog.providers(), "YouTube provider catalog returned.");
    }

    private DispatchExecutionResult records(Object value, String summary) {
        JsonNode payload = objectMapper.valueToTree(value);
        RecordsContent structured = new RecordsContent();
        List<JsonNode> records = new ArrayList<>();
        payload.forEach(records::add);
        structured.setRecords(records);
        structured.setTotal(payload.size());
        return DispatchExecutionResult.builder()
                .array(payload)
                .structuredContent(structured)
                .status("completed")
                .summary(summary)
                .build();
    }
}
