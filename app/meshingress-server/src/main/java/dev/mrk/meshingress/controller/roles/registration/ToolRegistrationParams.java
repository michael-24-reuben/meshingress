package dev.mrk.meshingress.controller.roles.registration;

public record ToolRegistrationParams(
        String phase,
        String toolId,
        String name,
        ToolRegistrationToolParams tool,
        ToolRegistrationLocalJarParams localJar,
        ToolRegistrationMavenParams maven,
        ToolRegistrationBundleParams bundle,
        ToolRegistrationNativeParams nativeTool,
        Boolean replace
) {
}

