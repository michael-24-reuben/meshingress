package dev.mrk.meshingress.auth;

public record McpAuthenticatedSession(
        String clientId,
        String subject
) {
}
