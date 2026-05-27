package dev.mrk.meshingress.runtime.artifacts;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public record MavenCoordinatesSource(
        String groupId,
        String artifactId,
        String version,
        List<URI> repositories
) implements ToolArtifactSource {
    public MavenCoordinatesSource {
        groupId = requireText(groupId, "groupId");
        artifactId = requireText(artifactId, "artifactId");
        version = requireText(version, "version");
        repositories = repositories == null ? List.of() : List.copyOf(repositories);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
