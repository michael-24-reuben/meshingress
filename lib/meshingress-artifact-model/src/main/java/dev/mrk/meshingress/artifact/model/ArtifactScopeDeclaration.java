package dev.mrk.meshingress.artifact.model;

import java.util.List;

/**
 * Represents the scope declarations of a Meshingress artifact across its lifecycle.
 *
 * <p>The lifecycle transitions the scope state as follows:
 * <ul>
 *   <li><b>Upload Phase:</b> The artifact claims {@code requestedScopes} which represent what the developer declares the tool needs.</li>
 *   <li><b>Assessment Phase:</b> Automated scanners and analyzers infer {@code inferredScopes} from bytecode reachability and call graphs.</li>
 *   <li><b>Review/Approval Phase:</b> A reviewer reviews both the requested and inferred scopes, decides which scopes are safe, and designates them as {@code approvedScopes}. Any rejected scopes are documented in {@code deniedScopes}.</li>
 *   <li><b>Publication/Runtime Phase:</b> The signed publication record embeds this declaration, and the MCP runtime enforces {@code approvedScopes} as the sole authority.</li>
 * </ul>
 *
 * @param requestedScopes Scopes explicitly claimed by the artifact during upload.
 * @param inferredScopes Scopes automatically detected by the static/bytecode analysis scanner.
 * @param approvedScopes The subset of scopes explicitly approved by a reviewer; this is the runtime authority.
 * @param deniedScopes Scopes explicitly rejected by the reviewer, accompanied by justification reasons.
 */
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
