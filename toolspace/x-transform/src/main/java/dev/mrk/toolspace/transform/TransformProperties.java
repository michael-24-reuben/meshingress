package dev.mrk.toolspace.transform;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meshingress.transform")
public record TransformProperties(String nodeExecutable, String backendDirectory, Long timeoutMs) {
    public TransformProperties {
        nodeExecutable = blankOr(nodeExecutable, "node");
        backendDirectory = blankOr(backendDirectory, "toolspace/x-transform/src/main/resources/transform-backend");
        timeoutMs = timeoutMs == null || timeoutMs <= 0 ? 120_000L : Math.min(timeoutMs, 900_000L);
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
