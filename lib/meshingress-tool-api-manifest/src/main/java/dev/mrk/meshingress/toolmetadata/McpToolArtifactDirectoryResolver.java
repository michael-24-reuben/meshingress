package dev.mrk.meshingress.toolmetadata;

import java.nio.file.Path;
import java.util.Optional;

public interface McpToolArtifactDirectoryResolver {
    Optional<Path> artifactDirectory(Class<?> toolClass);
}
