package dev.mrk.meshingress.artifact.model;

import java.time.OffsetDateTime;
import java.util.List;

public record ArtifactRecord(
        ArtifactCoordinate coordinate,
        MeshingressArtifactType type,
        ArtifactTrustStatus trustStatus,
        String artifactUri,
        ArtifactChecksum artifactChecksum,
        List<ArtifactFileEntry> files,
        ArtifactScopeDeclaration scopes,
        ArtifactAssessmentSummary assessment,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public ArtifactRecord {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate must not be null");
        }
        type = type == null ? MeshingressArtifactType.TOOL_MODULE : type;
        trustStatus = trustStatus == null ? ArtifactTrustStatus.RECEIVED : trustStatus;
        artifactUri = artifactUri == null ? "" : artifactUri;
        files = files == null ? List.of() : List.copyOf(files);
        scopes = scopes == null ? ArtifactScopeDeclaration.empty() : scopes;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public ArtifactRecord withTrustStatus(ArtifactTrustStatus next, ArtifactAssessmentSummary nextAssessment) {
        return new ArtifactRecord(
                coordinate,
                type,
                next,
                artifactUri,
                artifactChecksum,
                files,
                scopes,
                nextAssessment == null ? assessment : nextAssessment,
                createdAt,
                OffsetDateTime.now()
        );
    }

    public ArtifactRecord withScopes(ArtifactScopeDeclaration nextScopes, ArtifactTrustStatus nextStatus) {
        return new ArtifactRecord(
                coordinate,
                type,
                nextStatus,
                artifactUri,
                artifactChecksum,
                files,
                nextScopes,
                assessment,
                createdAt,
                OffsetDateTime.now()
        );
    }
}
