package dev.mrk.meshingress.runtime.artifacts;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public record ToolArtifactResolutionContext(
        Path localRepository,
        Path cacheDirectory,
        boolean allowRemoteRepositories,
        List<URI> allowedRepositories
) {
    public ToolArtifactResolutionContext {
        localRepository = localRepository == null
                ? Path.of(System.getProperty("user.home"), ".m2", "repository")
                : localRepository;
        cacheDirectory = cacheDirectory == null ? Path.of("data", "tool-artifacts") : cacheDirectory;
        allowedRepositories = allowedRepositories == null ? List.of() : List.copyOf(allowedRepositories);
    }

    public static ToolArtifactResolutionContext defaults() {
        return new ToolArtifactResolutionContext(null, null, false, List.of());
    }
}
