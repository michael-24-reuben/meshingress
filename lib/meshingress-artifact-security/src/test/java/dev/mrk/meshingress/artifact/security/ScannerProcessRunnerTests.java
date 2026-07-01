package dev.mrk.meshingress.artifact.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScannerProcessRunnerTests {
    private final ScannerProcessRunner runner = new ScannerProcessRunner();

    @TempDir
    Path tempDir;

    @Test
    void capturesSuccessfulProcessOutput() {
        ScannerProcessResult result = runner.run(request("success-scanner", "success", Duration.ofSeconds(5)));

        assertThat(result.status()).isEqualTo(ScannerProcessStatus.SUCCEEDED);
        assertThat(result.exitCode()).isZero();
        assertThat(result.successfulExit()).isTrue();
        assertThat(result.stdout()).contains("scanner-ok");
        assertThat(result.stderr()).contains("scanner-warning");
        assertThat(result.failureReason()).isBlank();
    }

    @Test
    void normalizesNonZeroExit() {
        ScannerProcessResult result = runner.run(request("exit-scanner", "exit", Duration.ofSeconds(5)));

        assertThat(result.status()).isEqualTo(ScannerProcessStatus.NON_ZERO_EXIT);
        assertThat(result.exitCode()).isEqualTo(7);
        assertThat(result.successfulExit()).isFalse();
        assertThat(result.failureFinding().code()).isEqualTo("SCANNER_PROCESS_NON_ZERO_EXIT");
        assertThat(result.toFailureScannerResult("1.0-test", ScannerFailurePolicy.BLOCK, null).status())
                .isEqualTo(ScannerStatus.BLOCKED);
    }

    @Test
    void normalizesMissingExecutable() {
        ScannerProcessResult result = runner.run(new ScannerProcessRequest(
                "missing-scanner",
                List.of(tempDir.resolve("definitely-missing-scanner.exe").toString(), "--version"),
                tempDir,
                Duration.ofSeconds(5)
        ));

        assertThat(result.status()).isEqualTo(ScannerProcessStatus.START_FAILED);
        assertThat(result.exitCode()).isNull();
        assertThat(result.failureFinding().code()).isEqualTo("SCANNER_PROCESS_START_FAILED");
        assertThat(result.toFailureScannerResult("1.0-test", ScannerFailurePolicy.REVIEW, null).status())
                .isEqualTo(ScannerStatus.REVIEW);
    }

    @Test
    void terminatesTimedOutProcess() {
        ScannerProcessResult result = runner.run(request("timeout-scanner", "sleep", Duration.ofMillis(150)));

        assertThat(result.status()).isEqualTo(ScannerProcessStatus.TIMED_OUT);
        assertThat(result.exitCode()).isNull();
        assertThat(result.duration()).isGreaterThanOrEqualTo(Duration.ofMillis(100));
        assertThat(result.failureFinding().code()).isEqualTo("SCANNER_PROCESS_TIMEOUT");
        assertThat(result.toFailureScannerResult("1.0-test", ScannerFailurePolicy.IGNORE, null).status())
                .isEqualTo(ScannerStatus.FAILED);
    }

    private ScannerProcessRequest request(String scanner, String mode, Duration timeout) {
        return new ScannerProcessRequest(
                scanner,
                List.of(javaExecutable().toString(), "-cp", System.getProperty("java.class.path"),
                        CommandMain.class.getName(), mode),
                tempDir,
                timeout
        );
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    public static final class CommandMain {
        public static void main(String[] args) throws Exception {
            String mode = args.length == 0 ? "success" : args[0];
            switch (mode) {
                case "success" -> {
                    System.out.println("scanner-ok");
                    System.err.println("scanner-warning");
                }
                case "exit" -> {
                    System.err.println("scanner-failed");
                    System.exit(7);
                }
                case "sleep" -> Thread.sleep(10_000);
                default -> throw new IllegalArgumentException("unknown mode: " + mode);
            }
        }
    }
}
