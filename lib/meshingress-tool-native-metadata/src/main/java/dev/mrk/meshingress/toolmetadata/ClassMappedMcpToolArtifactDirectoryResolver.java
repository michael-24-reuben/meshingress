package dev.mrk.meshingress.toolmetadata;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class ClassMappedMcpToolArtifactDirectoryResolver implements McpToolArtifactDirectoryResolver {

    private final Map<Class<?>, Path> artifactDirectories = Collections.synchronizedMap(new WeakHashMap<>());
    private final McpToolArtifactDirectoryResolver fallback;

    public ClassMappedMcpToolArtifactDirectoryResolver() {
        this(new CodeSourceMcpToolArtifactDirectoryResolver());
    }

    public ClassMappedMcpToolArtifactDirectoryResolver(McpToolArtifactDirectoryResolver fallback) {
        this.fallback = fallback;
    }

    public void register(Class<?> toolClass, Path artifactDirectory) {
        if (toolClass == null) {
            throw new IllegalArgumentException("toolClass must not be null");
        }
        if (artifactDirectory == null) {
            throw new IllegalArgumentException("artifactDirectory must not be null");
        }
        artifactDirectories.put(toolClass, artifactDirectory.toAbsolutePath().normalize());
    }

    public void unregister(Class<?> toolClass) {
        if (toolClass != null) {
            artifactDirectories.remove(toolClass);
        }
    }

    @Override
    public Optional<Path> artifactDirectory(Class<?> toolClass) {
        if (toolClass == null) {
            return Optional.empty();
        }
        Path mapped = artifactDirectories.get(toolClass);
        if (mapped != null) {
            return Optional.of(mapped);
        }
        return fallback == null ? Optional.empty() : fallback.artifactDirectory(toolClass);
    }
}
