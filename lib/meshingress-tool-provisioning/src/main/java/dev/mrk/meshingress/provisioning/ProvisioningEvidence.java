package dev.mrk.meshingress.provisioning;

import java.time.Duration;

public record ProvisioningEvidence(
        String phase,
        String summary,
        Duration duration,
        boolean successful
) {
    public ProvisioningEvidence {
        phase = normalize(phase, "unknown");
        summary = summary == null ? "" : summary.trim();
        duration = duration == null ? Duration.ZERO : duration;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
