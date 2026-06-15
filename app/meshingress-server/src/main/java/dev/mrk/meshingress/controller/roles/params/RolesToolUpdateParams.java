package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parameters for roles/tools/update.")
public record RolesToolUpdateParams(
        @Schema(description = "Name of the existing tool to update.", example = "architect.entries.copy", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Patch to apply to the registry tool entry.", requiredMode = Schema.RequiredMode.REQUIRED)
        ToolPatchParams patch
) {
}

