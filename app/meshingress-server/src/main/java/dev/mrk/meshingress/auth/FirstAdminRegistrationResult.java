package dev.mrk.meshingress.auth;

public record FirstAdminRegistrationResult(
        String adminId,
        String username,
        String email,
        String displayName,
        GeneratedMcpAuth generatedAuth
) {
}
