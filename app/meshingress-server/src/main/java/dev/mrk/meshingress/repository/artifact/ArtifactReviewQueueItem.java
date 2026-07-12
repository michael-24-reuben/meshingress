package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.security.ScannerResult;

import java.util.List;

public record ArtifactReviewQueueItem(
        ArtifactRecord artifact,
        List<ScannerResult> assessment
) {
    public ArtifactReviewQueueItem {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        assessment = assessment == null ? List.of() : List.copyOf(assessment);
    }
}
