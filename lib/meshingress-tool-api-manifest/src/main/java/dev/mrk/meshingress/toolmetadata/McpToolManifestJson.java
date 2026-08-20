package dev.mrk.meshingress.toolmetadata;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class McpToolManifestJson {

    public static final int SCHEMA_VERSION = 2;
    public static final String MANIFEST_PATH = "META-INF/meshingress/tool-manifest.json";
    public static final String RESOURCES_MANIFEST_PATH = "resources/tool-manifest.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private McpToolManifestJson() { }

    public static McpToolNativeMetadata read(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            ObjectNode document = (ObjectNode) OBJECT_MAPPER.readTree(input);
            String inlineReadme = document.path("readme").path("markdown").asString("");
            document.remove("readme");
            int schemaVersion = document.path("schemaVersion").asInt(1);
            if (schemaVersion == 1) {
                String toolId = document.path("toolId").asString("");
                List<ToolLink> links = document.has("links")
                        ? OBJECT_MAPPER.treeToValue(document.get("links"), new TypeReference<List<ToolLink>>() { })
                        : List.of();
                document.remove("toolId");
                document.remove("links");
                McpToolNativeMetadata legacy = OBJECT_MAPPER.treeToValue(document, McpToolNativeMetadata.class);
                return new McpToolNativeMetadata(SCHEMA_VERSION, toolId, legacy.properties(), legacy.requirements(), links,
                        ToolReadme.inline(inlineReadme));
            }
            if (schemaVersion != SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported tool manifest schema: " + schemaVersion);
            }
            McpToolNativeMetadata parsed = OBJECT_MAPPER.treeToValue(document, McpToolNativeMetadata.class);
            return new McpToolNativeMetadata(parsed.schemaVersion(), parsed.metadata(), parsed.properties(), parsed.requirements(),
                    ToolReadme.inline(inlineReadme));
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
