package dev.mrk.meshingress.controller.roles.registration;

public record ToolRegistrationRequest(
        String toolId,
        ToolRegistrationPhase phase,
        LocalJarSpec localJar,
        MavenCoordinatesSpec maven,
        BundleSpec bundle,
        NativeSpec nativeTool,
        boolean replace
) {
}
