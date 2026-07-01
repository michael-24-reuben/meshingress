package dev.mrk.meshingress.artifact.security;

import java.time.Duration;

public record GrypeScannerOptions(
        String executable,
        String scannerVersion,
        Duration timeout,
        ScannerFailurePolicy failurePolicy,
        GrypeSeverity blockSeverityThreshold,
        GrypeSeverity reviewSeverityThreshold,
        int maxOutputChars
) {
    public GrypeScannerOptions {
        executable = executable == null || executable.isBlank() ? "grype" : executable.trim();
        scannerVersion = scannerVersion == null ? "" : scannerVersion.trim();
        timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? ScannerPipelineStage.DEFAULT_TIMEOUT
                : timeout;
        failurePolicy = failurePolicy == null ? ScannerFailurePolicy.BLOCK : failurePolicy;
        blockSeverityThreshold = blockSeverityThreshold == null ? GrypeSeverity.CRITICAL : blockSeverityThreshold;
        reviewSeverityThreshold = reviewSeverityThreshold == null ? GrypeSeverity.LOW : reviewSeverityThreshold;
        maxOutputChars = maxOutputChars <= 0 ? ScannerProcessRequest.DEFAULT_MAX_OUTPUT_CHARS : maxOutputChars;
    }

    public static GrypeScannerOptions defaults() {
        return new GrypeScannerOptions(
                "grype",
                "",
                ScannerPipelineStage.DEFAULT_TIMEOUT,
                ScannerFailurePolicy.BLOCK,
                GrypeSeverity.CRITICAL,
                GrypeSeverity.LOW,
                ScannerProcessRequest.DEFAULT_MAX_OUTPUT_CHARS
        );
    }
}
