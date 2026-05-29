package dev.mrk.meshingress.artifact.model;

public record DeniedScope(String scope, String reason) {
    public DeniedScope {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        reason = reason == null ? "" : reason.trim();
    }
}
