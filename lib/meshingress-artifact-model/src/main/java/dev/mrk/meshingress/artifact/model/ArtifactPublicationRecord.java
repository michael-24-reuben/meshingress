package dev.mrk.meshingress.artifact.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        PublicationEligibilityDecision eligibilityDecision,
        boolean revoked,
        OffsetDateTime publishedAt,
        String signatureKeyId,
        String signatureAlgorithm,
        String signature
) {
    public ArtifactPublicationRecord(
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
            String signatureKeyId,
            String signatureAlgorithm,
            String signature
    ) {
        this(
                coordinate,
                type,
                trustStatus,
                artifactUri,
                artifactChecksum,
                scopePolicy,
                scanSummary,
                provenance,
                PublicationEligibilityDecision.accepted(Map.of()),
                revoked,
                publishedAt,
                signatureKeyId,
                signatureAlgorithm,
                signature
        );
    }

    public ArtifactPublicationRecord {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate must not be null");
        }
        type = type == null ? MeshingressArtifactType.TOOL_MODULE : type;
        trustStatus = trustStatus == null ? ArtifactTrustStatus.REVIEW_PENDING : trustStatus;
        artifactUri = requireText(artifactUri, "artifactUri");
        if (artifactChecksum == null) {
            throw new IllegalArgumentException("artifactChecksum must not be null");
        }
        scopePolicy = scopePolicy == null ? ArtifactScopeDeclaration.empty() : scopePolicy;
        scanSummary = scanSummary == null
                ? new ArtifactAssessmentSummary("unknown", java.util.List.of(), 0, Map.of())
                : scanSummary;
        provenance = provenance == null ? ArtifactProvenance.empty() : provenance;
        eligibilityDecision = eligibilityDecision == null
                ? PublicationEligibilityDecision.accepted(Map.of())
                : eligibilityDecision;
        publishedAt = publishedAt == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : publishedAt.withOffsetSameInstant(ZoneOffset.UTC);
        signatureKeyId = signatureKeyId == null ? "" : signatureKeyId.trim();
        signatureAlgorithm = signatureAlgorithm == null ? "" : signatureAlgorithm.trim();
        signature = signature == null ? "" : signature.trim();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
