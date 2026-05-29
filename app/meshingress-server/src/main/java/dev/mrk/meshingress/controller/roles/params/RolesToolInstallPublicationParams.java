package dev.mrk.meshingress.controller.roles.params;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;

public record RolesToolInstallPublicationParams(
        String toolId,
        ArtifactPublicationRecord publication
) {
}
