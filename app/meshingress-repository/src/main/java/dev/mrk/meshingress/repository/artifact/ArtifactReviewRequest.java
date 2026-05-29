package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.DeniedScope;

import java.util.List;

public record ArtifactReviewRequest(
        List<String> approvedScopes,
        List<DeniedScope> deniedScopes,
        ArtifactTrustStatus trustStatus,
        String reviewer,
        String notes
) {
    public ArtifactReviewRequest {
        approvedScopes = approvedScopes == null ? List.of() : List.copyOf(approvedScopes);
        deniedScopes = deniedScopes == null ? List.of() : List.copyOf(deniedScopes);
        reviewer = reviewer == null ? "" : reviewer.trim();
        notes = notes == null ? "" : notes.trim();
    }

    static ArtifactReviewRequest empty() {
        return new ArtifactReviewRequest(List.of(), List.of(), null, "", "");
    }
}
