package dev.mrk.meshingress.artifact.security;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrypeScannerAdapterTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void parsesGrypeMatchesIntoFindingsAndBlocksAtConfiguredThreshold() throws Exception {
        Files.writeString(tempDir.resolve("sample.jar"), "jar");
        GrypeScannerAdapter adapter = new GrypeScannerAdapter(
                new StaticRunner(grypeJson(), ScannerProcessStatus.SUCCEEDED, 0, ""),
                objectMapper,
                new GrypeScannerOptions(
                        "grype",
                        "0.115.0-test",
                        Duration.ofSeconds(5),
                        ScannerFailurePolicy.BLOCK,
                        GrypeSeverity.HIGH,
                        GrypeSeverity.LOW,
                        ScannerProcessRequest.DEFAULT_MAX_OUTPUT_CHARS
                )
        );

        ScannerResult result = adapter.scan(request());

        assertThat(result.scanner()).isEqualTo(GrypeScannerAdapter.SCANNER_NAME);
        assertThat(result.scannerVersion()).isEqualTo("0.115.0-test");
        assertThat(result.status()).isEqualTo(ScannerStatus.BLOCKED);
        assertThat(result.findings()).hasSize(2);
        assertThat(result.findings().getFirst().code()).isEqualTo("CVE-2026-0001");
        assertThat(result.findings().getFirst().severity()).isEqualTo("CRITICAL");
        assertThat(result.findings().getFirst().path()).isEqualTo("BOOT-INF/lib/log4j-core-2.14.1.jar");
        assertThat(result.rawSummary()).containsEntry("matchCount", 2);
        assertThat(result.rawSummary()).containsEntry("rawReport", "grype-vulnerability-scanner.json");
        assertThat(result.rawReportPath()).isRegularFile();
    }

    @Test
    void returnsFailureEvidenceWhenGrypeCannotStart() throws Exception {
        Files.writeString(tempDir.resolve("sample.jar"), "jar");
        GrypeScannerAdapter adapter = new GrypeScannerAdapter(
                new StaticRunner("", ScannerProcessStatus.START_FAILED, null, "CreateProcess error=2"),
                objectMapper,
                new GrypeScannerOptions(
                        "missing-grype",
                        "",
                        Duration.ofSeconds(5),
                        ScannerFailurePolicy.REVIEW,
                        GrypeSeverity.CRITICAL,
                        GrypeSeverity.LOW,
                        ScannerProcessRequest.DEFAULT_MAX_OUTPUT_CHARS
                )
        );

        ScannerResult result = adapter.scan(request());

        assertThat(result.status()).isEqualTo(ScannerStatus.REVIEW);
        assertThat(result.findings()).singleElement()
                .extracting(Finding::code)
                .isEqualTo("SCANNER_PROCESS_START_FAILED");
        assertThat(result.rawReportPath()).isRegularFile();
    }

    private ScannerRequest request() {
        return new ScannerRequest(
                new ArtifactCoordinate("dev.mrk.tools", "sample", "1.0.0", null, "jar"),
                tempDir.resolve("sample.jar"),
                tempDir,
                tempDir.resolve("assessment"),
                List.of()
        );
    }

    private String grypeJson() {
        return """
                {
                  "matches": [
                    {
                      "vulnerability": {
                        "id": "CVE-2026-0001",
                        "severity": "Critical",
                        "description": "Remote code execution in test package."
                      },
                      "artifact": {
                        "name": "log4j-core",
                        "version": "2.14.1",
                        "purl": "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1",
                        "locations": [
                          { "path": "BOOT-INF/lib/log4j-core-2.14.1.jar" }
                        ]
                      }
                    },
                    {
                      "vulnerability": {
                        "id": "GHSA-2026-low",
                        "severity": "Low"
                      },
                      "artifact": {
                        "name": "commons-io",
                        "version": "2.8.0",
                        "locations": [
                          { "path": "BOOT-INF/lib/commons-io-2.8.0.jar" }
                        ]
                      }
                    }
                  ],
                  "source": { "type": "file" },
                  "descriptor": { "name": "sample.jar" }
                }
                """;
    }

    private static final class StaticRunner extends ScannerProcessRunner {
        private final String stdout;
        private final ScannerProcessStatus status;
        private final Integer exitCode;
        private final String failureReason;

        private StaticRunner(String stdout, ScannerProcessStatus status, Integer exitCode, String failureReason) {
            this.stdout = stdout;
            this.status = status;
            this.exitCode = exitCode;
            this.failureReason = failureReason;
        }

        @Override
        public ScannerProcessResult run(ScannerProcessRequest request) {
            return new ScannerProcessResult(
                    request.scanner(),
                    request.command(),
                    request.workingDirectory(),
                    request.timeout(),
                    Duration.ofMillis(25),
                    status,
                    exitCode,
                    stdout,
                    "",
                    false,
                    false,
                    failureReason
            );
        }
    }
}
