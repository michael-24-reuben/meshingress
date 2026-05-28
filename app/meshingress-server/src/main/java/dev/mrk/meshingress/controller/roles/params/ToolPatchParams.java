package dev.mrk.meshingress.controller.roles.params;

import tools.jackson.databind.JsonNode;

public record ToolPatchParams(
        String title,
        String description,
        Boolean enabled,
        String visibility,
        String handlerKey,
        JsonNode inputSchema,
        JsonNode outputSchema,
        JsonNode annotations
) {
}

