package dev.mrk.meshingress.controller.roles.registration;

import java.util.List;

public record ToolRegistrationMavenParams(
        String groupId,
        String artifactId,
        String version,
        List<String> repositories
) {
}

