package dev.mrk.meshingress.toolmetadata;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Optional;

public class CodeSourceMcpToolArtifactDirectoryResolver implements McpToolArtifactDirectoryResolver {

    @Override
    public Optional<Path> artifactDirectory(Class<?> toolClass) {
        if (toolClass == null || toolClass.getProtectionDomain() == null) {
            return Optional.empty();
        }
        CodeSource codeSource = toolClass.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return Optional.empty();
        }
        try {
            URI location = codeSource.getLocation().toURI();
            Path path = Path.of(location).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                return Optional.ofNullable(path.getParent());
            }
            return Optional.of(path);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
