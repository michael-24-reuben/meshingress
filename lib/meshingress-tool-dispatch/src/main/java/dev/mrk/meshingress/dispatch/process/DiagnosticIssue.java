package dev.mrk.meshingress.dispatch.process;

public record DiagnosticIssue(
        String severity,
        String code,
        String message,
        String path,
        String hint
) { }
