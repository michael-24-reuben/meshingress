package dev.mrk.meshingress.artifact.scope;

import java.util.List;

public record ScopeInferenceCatalog(
        String version,
        List<ScopeInferenceRule> rules
) {
    public void validate() {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Scope catalog version is required");
        }
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("Scope catalog must contain at least one rule");
        }
        rules.forEach(ScopeInferenceRule::validate);
    }
}
