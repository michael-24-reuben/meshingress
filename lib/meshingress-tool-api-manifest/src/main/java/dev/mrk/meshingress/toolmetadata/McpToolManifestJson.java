package dev.mrk.meshingress.toolmetadata;

import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class McpToolManifestJson {

    public static final int SCHEMA_VERSION = 1;
    public static final String MANIFEST_PATH = "META-INF/meshingress/tool-manifest.json";
    public static final String RESOURCES_MANIFEST_PATH = "resources/tool-manifest.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private McpToolManifestJson() {
    }

    public static McpToolNativeMetadata read(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return OBJECT_MAPPER.readValue(input, McpToolNativeMetadata.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read tool manifest from " + path + ": " + exception.getMessage(), exception);
        }
    }

    public static void write(McpToolNativeMetadata metadata, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, OBJECT_MAPPER.writeValueAsString(metadata));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write tool manifest to " + path + ": " + exception.getMessage(), exception);
        }
    }
}
