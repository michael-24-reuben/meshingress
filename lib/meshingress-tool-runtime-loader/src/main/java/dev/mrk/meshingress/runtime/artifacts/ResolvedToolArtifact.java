package dev.mrk.meshingress.runtime.artifacts;

import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ResolvedToolArtifact(
        ToolModuleId moduleId,
        Path mainJar,
        List<Path> runtimeClasspath,
        ToolArtifactSource source,
        Map<String, String> checksums,
        ToolModuleDescriptor descriptor
) {
    public ResolvedToolArtifact {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(mainJar, "mainJar must not be null");
        runtimeClasspath = runtimeClasspath == null ? List.of(mainJar) : List.copyOf(runtimeClasspath);
        checksums = checksums == null ? Map.of() : Map.copyOf(checksums);
    }
}
