package dev.mrk.meshingress.auth;

public record BootstrapAdminState(
        String username,
        String password,
        String source,
        String createdAt,
        String consumedAt
) {
}
