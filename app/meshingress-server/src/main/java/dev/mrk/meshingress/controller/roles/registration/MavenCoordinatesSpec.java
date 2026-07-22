package dev.mrk.meshingress.controller.roles.registration;

import java.net.URI;
import java.util.List;

public record MavenCoordinatesSpec(
        String groupId,
        String artifactId,
        String version,
        List<URI> repositories
) {
    public MavenCoordinatesSpec {
        repositories = repositories == null ? List.of() : List.copyOf(repositories);
    }
}

