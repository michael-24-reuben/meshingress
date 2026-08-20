package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Optional namespace filter for persisted tool-family contributions.")
public record RolesToolContributionListParams(
        @Schema(description = "One-name tool namespace to list.", example = "youtube") String namespace
) {
}
