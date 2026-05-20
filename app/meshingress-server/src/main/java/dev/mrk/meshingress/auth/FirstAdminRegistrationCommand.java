package dev.mrk.meshingress.auth;

public record FirstAdminRegistrationCommand(
        String username,
        String bootstrapPassword,
        String newPassword,
        String confirmationPassword,
        String email,
        String displayName
) {
}
