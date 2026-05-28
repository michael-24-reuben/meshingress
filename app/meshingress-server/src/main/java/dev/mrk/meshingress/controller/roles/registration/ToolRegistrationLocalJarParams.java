package dev.mrk.meshingress.controller.roles.registration;

public record ToolRegistrationLocalJarParams(
        String path,
        String jarPath,
        String checksumSha256,
        String sha256
) {
}

