package dev.mrk.meshingress.artifact.security;

public record Finding(
        String severity,
        String code,
        String message,
        String path
) {
    public Finding {
        severity = severity == null || severity.isBlank() ? "INFO" : severity.trim().toUpperCase();
        code = code == null ? "" : code.trim();
        message = message == null ? "" : message.trim();
        path = path == null ? "" : path.trim();
    }
}
