package dev.mrk.meshingress.scopes;

public record ScopeMetadata(
        ScopeCategory category,
        AccessMode accessMode,
        RiskLevel riskLevel,
        boolean auditRecommended,
        boolean privileged,
        String description
) {
}
