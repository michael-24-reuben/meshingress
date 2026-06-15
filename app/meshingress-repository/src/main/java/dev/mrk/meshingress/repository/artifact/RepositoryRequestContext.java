package dev.mrk.meshingress.repository.artifact;

public record RepositoryRequestContext(
        String authorization,
        String role,
        String actor,
        String requestId
) {
    public RepositoryRequestContext {
        authorization = clean(authorization);
        role = clean(role);
        actor = actor == null || actor.isBlank() ? "unknown" : actor.trim();
        requestId = clean(requestId);
    }

    static RepositoryRequestContext fromHeaders(String authorization, String role, String actor, String requestId) {
        return new RepositoryRequestContext(authorization, role, actor, requestId);
    }

    static RepositoryRequestContext system() {
        return new RepositoryRequestContext(null, "admin", "system", null);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
