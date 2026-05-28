package dev.mrk.meshingress.controller.roles.params;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record ToolDescriptorParams(
        String name,
        String title,
        String description,
        Integer version,
        Boolean enabled,
        String visibility,
        String handlerKey,
        JsonNode inputSchema,
        JsonNode outputSchema,
        JsonNode annotations,
        List<ToolFunctionParams> functions
) {
}

