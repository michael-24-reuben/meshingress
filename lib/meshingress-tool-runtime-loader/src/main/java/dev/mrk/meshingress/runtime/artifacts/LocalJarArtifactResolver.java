package dev.mrk.meshingress.runtime.artifacts;

import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LocalJarArtifactResolver implements ToolArtifactResolver {

    @Override
    public boolean supports(ToolArtifactSource source) {
        return source instanceof LocalJarSource;
    }

    @Override
    public ResolvedToolArtifact resolve(ToolArtifactSource source, ToolArtifactResolutionContext context) {
        LocalJarSource localJar = (LocalJarSource) source;
        Path jar = localJar.jarPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalArgumentException("Tool module JAR does not exist: " + jar);
        }
        ToolModuleId moduleId = new ToolModuleId(stripJarExtension(jar.getFileName().toString()));
        ToolModuleDescriptor descriptor = new ToolModuleDescriptor(
                moduleId,
                jar.getFileName().toString(),
                "local",
                List.of(),
                Map.of("source", "local-jar")
        );
        return new ResolvedToolArtifact(moduleId, jar, List.of(jar), localJar, Map.of(), descriptor);
    }

    private String stripJarExtension(String fileName) {
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }
}
