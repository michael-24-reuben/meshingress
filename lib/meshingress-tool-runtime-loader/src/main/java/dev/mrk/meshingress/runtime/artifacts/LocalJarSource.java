package dev.mrk.meshingress.runtime.artifacts;

import java.nio.file.Path;
import java.util.Objects;

public record LocalJarSource(Path jarPath) implements ToolArtifactSource {
    public LocalJarSource {
        Objects.requireNonNull(jarPath, "jarPath must not be null");
    }
}
