package dev.mrk.meshingress.artifact.model;

import java.util.List;

public record ArtifactScopeDeclaration(
        List<String> requestedScopes,
        List<String> inferredScopes,
        List<String> approvedScopes,
        List<DeniedScope> deniedScopes
) {
    public ArtifactScopeDeclaration {
        requestedScopes = normalize(requestedScopes);
        inferredScopes = normalize(inferredScopes);
        approvedScopes = normalize(approvedScopes);
        deniedScopes = deniedScopes == null ? List.of() : List.copyOf(deniedScopes);
    }

    public static ArtifactScopeDeclaration empty() {
        return new ArtifactScopeDeclaration(List.of(), List.of(), List.of(), List.of());
    }

    private static List<String> normalize(List<String> scopes) {
        if (scopes == null) {
            return List.of();
        }
        return scopes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
