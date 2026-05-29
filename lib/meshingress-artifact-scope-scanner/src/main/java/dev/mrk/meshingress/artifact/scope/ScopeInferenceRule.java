package dev.mrk.meshingress.artifact.scope;

import java.util.List;

public record ScopeInferenceRule(
        String id,
        String scope,
        List<ScopeMatcherDefinition> matchers
) {
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scope rule id is required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Scope rule scope is required for " + id);
        }
        if (matchers == null || matchers.isEmpty()) {
            throw new IllegalArgumentException("Scope rule must contain matchers: " + id);
        }
        matchers.forEach(matcher -> matcher.validate(id));
    }
}
