package dev.mrk.meshingress.controller.roles.registration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Local JAR registration source for a runtime tool module.")
public record ToolRegistrationLocalJarParams(
        @Schema(description = "Filesystem path to the local tool JAR. Either path or jarPath may be supplied.", example = "C:/tools/sample-tool.jar")
        String path,
        @Schema(description = "Legacy filesystem path to the local tool JAR. Prefer path for new callers.", example = "C:/tools/sample-tool.jar")
        String jarPath,
        @Schema(description = "Expected SHA-256 checksum for the JAR.", example = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
        String checksumSha256,
        @Schema(description = "Legacy SHA-256 checksum field. Prefer checksumSha256 for new callers.", example = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
        String sha256
) {
}

