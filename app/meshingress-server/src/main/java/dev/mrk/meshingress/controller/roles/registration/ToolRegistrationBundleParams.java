package dev.mrk.meshingress.controller.roles.registration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bundle-backed tool registration source.")
public record ToolRegistrationBundleParams(
        @Schema(description = "Bundle identifier that contains the tool.", example = "meshingress-tool-bundle")
        String bundleId,
        @Schema(description = "Artifact id inside the bundle.", example = "helloworld")
        String artifactId
) {
}

