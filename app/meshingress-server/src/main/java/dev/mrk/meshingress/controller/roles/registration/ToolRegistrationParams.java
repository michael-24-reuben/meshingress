package dev.mrk.meshingress.controller.roles.registration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Phase-aware parameters for roles/tools/register.")
public record ToolRegistrationParams(
        @Schema(description = "Registration lifecycle phase. Phase registration requires this field.", allowableValues = {"check", "install", "activate", "deactivate", "delete"}, example = "install", requiredMode = Schema.RequiredMode.REQUIRED)
        String phase,
        @Schema(description = "Stable registry tool id.", example = "sample.local")
        String toolId,
        @Schema(description = "Human-readable registration name.", example = "Sample Local Tool")
        String name,
        @Schema(description = "Existing tool reference for lifecycle operations.")
        ToolRegistrationToolParams tool,
        @Schema(description = "Local JAR source details.")
        ToolRegistrationLocalJarParams localJar,
        @Schema(description = "Maven coordinate source details.")
        ToolRegistrationMavenParams maven,
        @Schema(description = "Bundle source details.")
        ToolRegistrationBundleParams bundle,
        @Schema(description = "Native built-in source details.")
        ToolRegistrationNativeParams nativeTool,
        @Schema(description = "Replace an existing dynamic registration when true.", example = "false")
        Boolean replace
) {
}

