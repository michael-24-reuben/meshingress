package dev.mrk.toolspace.youtube;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpCacheResult;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.toolspace.youtube.catalog.YoutubeCapability;
import dev.mrk.toolspace.youtube.catalog.YoutubeCapabilityCatalog;
import dev.mrk.toolspace.youtube.catalog.YoutubeCapabilitiesContent;
import dev.mrk.toolspace.youtube.catalog.YoutubeProvider;
import dev.mrk.toolspace.youtube.catalog.YoutubeProvidersContent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
            description = "List implemented and planned YouTube function IDs, provider boundaries, authorization requirements, and delivery status.",
            outputTypes = YoutubeCapabilitiesContent.class
    )
    @McpCacheResult(ttlMs = 3600)
    public DispatchExecutionResult capabilities(McpCallContext context) {
        List<YoutubeCapability> capabilities = catalog.capabilities();
        return records(capabilities, new YoutubeCapabilitiesContent(capabilities), "YouTube capability catalog returned.");
    }

    @McpFunction(
            value = "providers",
            title = "List YouTube providers",
            description = "List the provider boundary for official YouTube APIs, local media, and future transcript or AI providers.",
            outputTypes = YoutubeProvidersContent.class
    )
    public DispatchExecutionResult providers(McpCallContext context) {
        List<YoutubeProvider> providers = catalog.providers();
        return records(providers, new YoutubeProvidersContent(providers), "YouTube provider catalog returned.");
    }

    private DispatchExecutionResult records(Object value, StructuredContent structured, String summary) {
        JsonNode payload = objectMapper.valueToTree(value);
        return DispatchExecutionResult.builder()
                .array(payload)
                .structuredContent(structured)
                .status("completed")
                .summary(summary)
                .build();
    }
}
