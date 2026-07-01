package dev.mrk.meshingress.artifact.security;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ScannerProcessRequest(
        String scanner,
        List<String> command,
        Path workingDirectory,
        Duration timeout,
        Map<String, String> environment,
        int maxOutputChars
) {
    public static final int DEFAULT_MAX_OUTPUT_CHARS = 262_144;

    public ScannerProcessRequest {
        scanner = scanner == null || scanner.isBlank() ? "unknown" : scanner.trim();
        if (command == null || command.isEmpty() || command.getFirst() == null || command.getFirst().isBlank()) {
            throw new IllegalArgumentException("command must include an executable");
        }
        command = command.stream()
                .map(argument -> argument == null ? "" : argument)
                .toList();
        timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? ScannerPipelineStage.DEFAULT_TIMEOUT
                : timeout;
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        maxOutputChars = maxOutputChars <= 0 ? DEFAULT_MAX_OUTPUT_CHARS : maxOutputChars;
    }

    public ScannerProcessRequest(
            String scanner,
            List<String> command,
            Path workingDirectory,
            Duration timeout
    ) {
        this(scanner, command, workingDirectory, timeout, Map.of(), DEFAULT_MAX_OUTPUT_CHARS);
    }
}
