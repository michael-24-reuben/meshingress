package dev.mrk.meshingress.artifact.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record ArtifactPublicationRecord(
        ArtifactCoordinate coordinate,
        MeshingressArtifactType type,
        ArtifactTrustStatus trustStatus,
        String artifactUri,
        ArtifactChecksum artifactChecksum,
        ArtifactScopeDeclaration scopePolicy,
        ArtifactAssessmentSummary scanSummary,
        ArtifactProvenance provenance,
        boolean revoked,
        OffsetDateTime publishedAt,
        String signatureAlgorithm,
        String signature
) {
    public ArtifactPublicationRecord {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate must not be null");
        }
        type = type == null ? MeshingressArtifactType.TOOL_MODULE : type;
        trustStatus = trustStatus == null ? ArtifactTrustStatus.REVIEW_PENDING : trustStatus;
        if (!trustStatus.installable()) {
            throw new IllegalArgumentException("publication record requires an installable trust status");
        }
        artifactUri = requireText(artifactUri, "artifactUri");
        if (artifactChecksum == null) {
            throw new IllegalArgumentException("artifactChecksum must not be null");
        }
        scopePolicy = scopePolicy == null ? ArtifactScopeDeclaration.empty() : scopePolicy;
        scanSummary = scanSummary == null
                ? new ArtifactAssessmentSummary("unknown", java.util.List.of(), 0, Map.of())
                : scanSummary;
        provenance = provenance == null ? ArtifactProvenance.empty() : provenance;
        publishedAt = publishedAt == null ? OffsetDateTime.now() : publishedAt;
        signatureAlgorithm = signatureAlgorithm == null ? "" : signatureAlgorithm;
        signature = signature == null ? "" : signature;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
