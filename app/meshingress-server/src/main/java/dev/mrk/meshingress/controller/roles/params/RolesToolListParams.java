package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parameters for roles/tools/list.")
public record RolesToolListParams(
        @Schema(description = "Include disabled registry entries in the listing.", example = "false")
        Boolean includeDisabled,
        @Schema(description = "Include private role-visible tools in the listing.", example = "false")
        Boolean includePrivate
) {
}

