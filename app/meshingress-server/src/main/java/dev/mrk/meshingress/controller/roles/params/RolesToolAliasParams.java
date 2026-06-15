package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parameters for roles/tools/alias.")
public record RolesToolAliasParams(
        @Schema(description = "Dynamic tool descriptor to register as an alias.", requiredMode = Schema.RequiredMode.REQUIRED)
        ToolDescriptorParams tool
) {
}

