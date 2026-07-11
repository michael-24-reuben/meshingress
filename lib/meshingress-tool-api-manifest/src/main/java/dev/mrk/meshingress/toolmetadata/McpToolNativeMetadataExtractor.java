package dev.mrk.meshingress.toolmetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class McpToolNativeMetadataExtractor {

    public McpToolNativeMetadata extract(Path quarantineDirectory) {
        if (quarantineDirectory == null || !Files.isDirectory(quarantineDirectory)) {
            return McpToolNativeMetadata.empty();
        }

        try (var stream = Files.walk(quarantineDirectory)) {
            List<Path> manifests = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isManifestPath)
                    .sorted()
                    .toList();
            if (manifests.isEmpty()) {
                return McpToolNativeMetadata.empty();
            }
            return McpToolManifestJson.read(manifests.getFirst());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to infer native tool metadata: " + exception.getMessage(), exception);
        }
    }

    private boolean isManifestPath(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.endsWith(McpToolManifestJson.MANIFEST_PATH)
                || normalized.endsWith(McpToolManifestJson.RESOURCES_MANIFEST_PATH);
    }
}
