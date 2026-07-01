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

    @McpToolProperty(
            name = "meshingress.sample.executable",
            description = "Executable used by the sample tool.",
            defaultValue = "sample-tool"
    )
    @McpToolReadme("# Annotation README")
    private static final class SampleTool {
    }
}
