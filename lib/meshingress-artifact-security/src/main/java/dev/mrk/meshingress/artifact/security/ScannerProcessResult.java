package dev.mrk.meshingress.artifact.security;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ScannerProcessResult(
        String scanner,
        List<String> command,
        Path workingDirectory,
        Duration timeout,
        Duration duration,
        ScannerProcessStatus status,
        Integer exitCode,
        String stdout,
        String stderr,
        boolean stdoutTruncated,
        boolean stderrTruncated,
        String failureReason
) {
    public ScannerProcessResult {
        scanner = scanner == null || scanner.isBlank() ? "unknown" : scanner.trim();
        command = command == null ? List.of() : List.copyOf(command);
        timeout = timeout == null ? ScannerPipelineStage.DEFAULT_TIMEOUT : timeout;
        duration = duration == null ? Duration.ZERO : duration;
        status = status == null ? ScannerProcessStatus.START_FAILED : status;
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        failureReason = failureReason == null ? "" : failureReason.trim();
    }

    public boolean successfulExit() {
        return status == ScannerProcessStatus.SUCCEEDED && Integer.valueOf(0).equals(exitCode);
    }

    public Finding failureFinding() {
        String code = switch (status) {
            case SUCCEEDED -> "SCANNER_PROCESS_SUCCEEDED";
            case NON_ZERO_EXIT -> "SCANNER_PROCESS_NON_ZERO_EXIT";
            case TIMED_OUT -> "SCANNER_PROCESS_TIMEOUT";
            case START_FAILED -> "SCANNER_PROCESS_START_FAILED";
            case INTERRUPTED -> "SCANNER_PROCESS_INTERRUPTED";
        };
        return new Finding(
                status == ScannerProcessStatus.TIMED_OUT ? "HIGH" : "MEDIUM",
                code,
                failureReason.isBlank() ? status.name() : failureReason,
                command.isEmpty() ? "" : command.getFirst()
        );
    }

    public ScannerResult toFailureScannerResult(
            String scannerVersion,
            ScannerFailurePolicy failurePolicy,
            Path rawReportPath
    ) {
        ScannerFailurePolicy policy = failurePolicy == null ? ScannerFailurePolicy.BLOCK : failurePolicy;
        Map<String, Object> processSummary = new LinkedHashMap<>();
        processSummary.put("command", command);
        processSummary.put("workingDirectory", workingDirectory == null ? "" : workingDirectory.toString());
        processSummary.put("timeout", timeout.toString());
        processSummary.put("duration", duration.toString());
        processSummary.put("status", status.name());
        processSummary.put("exitCode", exitCode == null ? "" : exitCode);
        processSummary.put("stdout", stdout);
        processSummary.put("stderr", stderr);
        processSummary.put("stdoutTruncated", stdoutTruncated);
        processSummary.put("stderrTruncated", stderrTruncated);
        processSummary.put("failureReason", failureReason);
        Map<String, Object> rawSummary = Map.of(
                "process", processSummary,
                "failurePolicy", policy.name()
        );
        return new ScannerResult(
                scanner,
                scannerVersion,
                policy.scannerStatusForFailure(),
                List.of(failureFinding()),
                rawSummary,
                rawReportPath
        );
    }
}
