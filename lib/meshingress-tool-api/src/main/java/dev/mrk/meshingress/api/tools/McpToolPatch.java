package dev.mrk.meshingress.api.tools;

import tools.jackson.databind.JsonNode;

public record McpToolPatch(
        String title,
        String description,
        Boolean enabled,
        ToolVisibility visibility,
        String handlerKey,
        JsonNode inputSchema,
        JsonNode outputSchema,
        JsonNode annotations
) {
}
