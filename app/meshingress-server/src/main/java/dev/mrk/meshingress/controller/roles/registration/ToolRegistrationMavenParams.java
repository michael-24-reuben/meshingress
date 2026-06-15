package dev.mrk.meshingress.controller.roles.registration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Maven coordinate registration source for a runtime tool module.")
public record ToolRegistrationMavenParams(
        @Schema(description = "Maven group id.", example = "dev.mrk.toolspace", requiredMode = Schema.RequiredMode.REQUIRED)
        String groupId,
        @Schema(description = "Maven artifact id.", example = "sample-tool", requiredMode = Schema.RequiredMode.REQUIRED)
        String artifactId,
        @Schema(description = "Maven artifact version.", example = "0.0.1", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @Schema(description = "Optional repository URLs to search in addition to the configured local repository.")
        List<String> repositories
) {
}

