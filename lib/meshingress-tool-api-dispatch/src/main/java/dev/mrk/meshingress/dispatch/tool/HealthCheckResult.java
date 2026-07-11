package dev.mrk.meshingress.dispatch.tool;

public record HealthCheckResult(
        String name,
        String status,
        String message,
        Long durationMs
) { }
