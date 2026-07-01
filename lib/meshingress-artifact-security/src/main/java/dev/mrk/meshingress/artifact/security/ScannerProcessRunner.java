package dev.mrk.meshingress.artifact.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScannerProcessRunner {
    private static final Duration DESTROY_WAIT = Duration.ofSeconds(5);

    public ScannerProcessResult run(ScannerProcessRequest request) {
        long started = System.nanoTime();
        ProcessBuilder builder = new ProcessBuilder(request.command());
        if (request.workingDirectory() != null) {
            builder.directory(request.workingDirectory().toFile());
        }
        builder.environment().putAll(request.environment());

        Process process;
        try {
            process = builder.start();
        } catch (IOException exception) {
            return failure(request, started, ScannerProcessStatus.START_FAILED, null, "", "", exception.getMessage());
        }

        CompletableFuture<String> stdout = readOutput(process.getInputStream(), request.maxOutputChars());
        CompletableFuture<String> stderr = readOutput(process.getErrorStream(), request.maxOutputChars());
        try {
            boolean completed = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(DESTROY_WAIT.toMillis(), TimeUnit.MILLISECONDS);
                return new ScannerProcessResult(
                        request.scanner(),
                        request.command(),
                        request.workingDirectory(),
                        request.timeout(),
                        elapsed(started),
                        ScannerProcessStatus.TIMED_OUT,
                        null,
                        awaitOutput(stdout),
                        awaitOutput(stderr),
                        outputTruncated(stdout, request.maxOutputChars()),
                        outputTruncated(stderr, request.maxOutputChars()),
                        "scanner process timed out after " + request.timeout()
                );
            }
            int exitCode = process.exitValue();
            ScannerProcessStatus status = exitCode == 0
                    ? ScannerProcessStatus.SUCCEEDED
                    : ScannerProcessStatus.NON_ZERO_EXIT;
            return new ScannerProcessResult(
                    request.scanner(),
                    request.command(),
                    request.workingDirectory(),
                    request.timeout(),
                    elapsed(started),
                    status,
                    exitCode,
                    awaitOutput(stdout),
                    awaitOutput(stderr),
                    outputTruncated(stdout, request.maxOutputChars()),
                    outputTruncated(stderr, request.maxOutputChars()),
                    exitCode == 0 ? "" : "scanner process exited with code " + exitCode
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return failure(request, started, ScannerProcessStatus.INTERRUPTED, null, awaitOutput(stdout), awaitOutput(stderr),
                    "scanner process interrupted");
        }
    }

    private static ScannerProcessResult failure(
            ScannerProcessRequest request,
            long started,
            ScannerProcessStatus status,
            Integer exitCode,
            String stdout,
            String stderr,
            String failureReason
    ) {
        return new ScannerProcessResult(
                request.scanner(),
                request.command(),
                request.workingDirectory(),
                request.timeout(),
                elapsed(started),
                status,
                exitCode,
                stdout,
                stderr,
                false,
                false,
                failureReason
        );
    }

    private static CompletableFuture<String> readOutput(java.io.InputStream inputStream, int maxOutputChars) {
        return CompletableFuture.supplyAsync(() -> {
            try (inputStream) {
                String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return trimOutput(output, maxOutputChars);
            } catch (IOException exception) {
                return "";
            }
        });
    }

    private static String awaitOutput(CompletableFuture<String> output) {
        try {
            return output.get(DESTROY_WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException exception) {
            return "";
        }
    }

    private static boolean outputTruncated(CompletableFuture<String> output, int maxOutputChars) {
        if (!output.isDone()) {
            return false;
        }
        String value = awaitOutput(output);
        return value.length() == maxOutputChars;
    }

    private static String trimOutput(String output, int maxOutputChars) {
        return output.length() <= maxOutputChars ? output : output.substring(0, maxOutputChars);
    }

    private static Duration elapsed(long started) {
        return Duration.ofNanos(System.nanoTime() - started);
    }
}
