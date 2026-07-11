package dev.mrk.meshingress.toolmetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolMetadataTests {

    @TempDir
    Path tempDir;

    @Test
    void resolvesAssignedPropertyValueFromRegisteredArtifactResources() throws Exception {
        Path artifactDirectory = tempDir.resolve("sample-tool");
        Path resourcesDirectory = artifactDirectory.resolve("resources");
        Files.createDirectories(resourcesDirectory);
        McpToolManifestJson.write(McpToolNativeMetadata.fromManifest(new SampleManifest()), resourcesDirectory.resolve("tool-manifest.json"));
        Files.writeString(resourcesDirectory.resolve("application.yaml"), """
                # generated
                meshingress.sample.executable: "custom-tool"
                """);
        Files.writeString(resourcesDirectory.resolve("README.md"), "# Runtime README\n");

        McpToolMetadata metadata = new McpToolMetadata();
        metadata.registerArtifactDirectory(SampleTool.class, artifactDirectory);

        McpToolPropertyMetadata property = metadata.toolProperty(
                SampleTool.class,
                "meshingress.sample.executable"
        );

        assertThat(metadata.toolProperties(SampleTool.class)).hasSize(1);
        assertThat(property.defaultValue()).isEqualTo("sample-tool");
        assertThat(property.assignedValue()).isEqualTo("custom-tool");
        assertThat(property.value()).isEqualTo("custom-tool");
        assertThat(property.description()).isEqualTo("Executable used by the sample tool.");
        assertThat(metadata.toolReadme(SampleTool.class)).isEqualTo("# Runtime README");
    }

    @Test
    void cachesMetadataUntilEvicted() throws Exception {
        Path artifactDirectory = tempDir.resolve("sample-tool");
        Path resourcesDirectory = artifactDirectory.resolve("resources");
        Files.createDirectories(resourcesDirectory);
        McpToolManifestJson.write(McpToolNativeMetadata.fromManifest(new SampleManifest()), resourcesDirectory.resolve("tool-manifest.json"));
        Path applicationYaml = resourcesDirectory.resolve("application.yaml");
        Files.writeString(applicationYaml, "meshingress.sample.executable: \"first\"\n");

        McpToolMetadata metadata = new McpToolMetadata();
        metadata.registerArtifactDirectory(SampleTool.class, artifactDirectory);

        assertThat(metadata.toolProperty(SampleTool.class, "meshingress.sample.executable").value())
                .isEqualTo("first");

        Files.writeString(applicationYaml, "meshingress.sample.executable: \"second\"\n");

        assertThat(metadata.toolProperty(SampleTool.class, "meshingress.sample.executable").value())
                .isEqualTo("first");

        metadata.evict(SampleTool.class);

        assertThat(metadata.toolProperty(SampleTool.class, "meshingress.sample.executable").value())
                .isEqualTo("second");
    }

    @Test
    void resolvesMetadataFromRegisteredManifestWhenArtifactDirectoryIsAbsent() {
        McpToolMetadata metadata = new McpToolMetadata();
        metadata.registerManifest(SampleTool.class, new SampleManifest());

        assertThat(metadata.toolProperty(SampleTool.class, "meshingress.sample.executable").value())
                .isEqualTo("sample-tool");
        assertThat(metadata.toolReadme(SampleTool.class)).isEqualTo("# Manifest README");
    }

    private static final class SampleTool {
    }

    private static final class SampleManifest implements McpToolManifestDefinition {

        @Override
        public String toolId() {
            return "sample.tool";
        }

        @Override
        public java.util.List<ToolProperty> properties() {
            return java.util.List.of(
                    ToolProperty.string("meshingress.sample.executable")
                            .description("Executable used by the sample tool.")
                            .defaultValue("sample-tool")
            );
        }

        @Override
        public String readmeMarkdown() {
            return "# Manifest README";
        }
    }
}
