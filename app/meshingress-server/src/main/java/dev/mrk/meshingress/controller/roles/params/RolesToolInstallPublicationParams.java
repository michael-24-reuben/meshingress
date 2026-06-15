package dev.mrk.meshingress.controller.roles.params;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parameters for roles/tools/installPublication.")
public record RolesToolInstallPublicationParams(
        @Schema(description = "Optional tool id override to use during installation.", example = "sample.tool")
        String toolId,
        @Schema(description = "Signed artifact publication to install.", requiredMode = Schema.RequiredMode.REQUIRED)
        ArtifactPublicationRecord publication
) {
}
