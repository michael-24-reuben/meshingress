package dev.mrk.meshingress.artifact.security;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ScannerResult(
        String scanner,
        String scannerVersion,
        ScannerStatus status,
        List<Finding> findings,
        Map<String, Object> rawSummary,
        Path rawReportPath
) {
    public ScannerResult {
        scanner = scanner == null || scanner.isBlank() ? "unknown" : scanner.trim();
        scannerVersion = scannerVersion == null ? "" : scannerVersion.trim();
        status = status == null ? ScannerStatus.FAILED : status;
        findings = findings == null ? List.of() : List.copyOf(findings);
        rawSummary = rawSummary == null ? Map.of() : Map.copyOf(rawSummary);
    }
}
