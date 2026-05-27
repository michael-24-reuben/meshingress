package dev.mrk.meshingress.runtime.artifacts;

import java.nio.file.Path;
import java.util.Objects;

public record PluginDirectorySource(Path directory) implements ToolArtifactSource {
    public PluginDirectorySource {
        Objects.requireNonNull(directory, "directory must not be null");
    }
}
