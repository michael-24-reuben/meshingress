package dev.mrk.meshingress.artifact.model;

import java.util.List;
import java.util.Map;

public record ArtifactAssessmentSummary(
        String status,
        List<String> scanners,
        int findingCount,
        Map<String, Object> summary
) {
    public ArtifactAssessmentSummary {
        status = requireText(status, "status");
        scanners = scanners == null ? List.of() : List.copyOf(scanners);
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
