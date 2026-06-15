package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parameters for roles/tools/check.")
public record RolesToolCheckParams(
        @Schema(description = "Proposed dynamic tool descriptor to validate.", requiredMode = Schema.RequiredMode.REQUIRED)
        ToolDescriptorParams tool,
        @Schema(description = "Validation mode. Use update to validate changes to an existing tool.", allowableValues = {"create", "update"}, example = "create")
        String mode
) {
}

