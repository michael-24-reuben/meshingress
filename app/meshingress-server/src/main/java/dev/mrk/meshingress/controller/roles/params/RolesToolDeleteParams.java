package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parameters for roles/tools/delete.")
public record RolesToolDeleteParams(
        @Schema(description = "Name of the tool to disable.", example = "architect.entries.copy", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Deletion mode. The current server supports disable only.", allowableValues = "disable", example = "disable")
        String mode
) {
}

