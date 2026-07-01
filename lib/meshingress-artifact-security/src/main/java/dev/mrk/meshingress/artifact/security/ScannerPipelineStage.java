package dev.mrk.meshingress.artifact.security;

import java.time.Duration;
import java.util.Map;

public record ScannerPipelineStage(
        String scanner,
        String expectedVersion,
        boolean required,
        Duration timeout,
        ScannerFailurePolicy failurePolicy
) {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public ScannerPipelineStage {
        if (scanner == null || scanner.isBlank()) {
            throw new IllegalArgumentException("scanner is required");
        }
        scanner = scanner.trim();
        expectedVersion = expectedVersion == null ? "" : expectedVersion.trim();
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? DEFAULT_TIMEOUT : timeout;
        failurePolicy = failurePolicy == null ? ScannerFailurePolicy.BLOCK : failurePolicy;
    }

    public Map<String, Object> toSummary() {
        return Map.of(
                "scanner", scanner,
                "expectedVersion", expectedVersion,
                "required", required,
                "timeout", timeout.toString(),
                "failurePolicy", failurePolicy.name()
        );
    }
}
