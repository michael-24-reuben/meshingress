package dev.mrk.meshingress.controller.roles.registration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Existing registered tool reference.")
public record ToolRegistrationToolParams(
        @Schema(description = "Registered tool name.", example = "architect.entries.list")
        String name
) {
}

