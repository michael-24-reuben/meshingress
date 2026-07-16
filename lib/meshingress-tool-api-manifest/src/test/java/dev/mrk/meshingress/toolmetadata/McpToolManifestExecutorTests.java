package dev.mrk.meshingress.toolmetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolManifestExecutorTests {

    @TempDir
    Path tempDir;

    @Test
    void writesStaticManifestFromNoArgumentManifestClass() {
        Path output = tempDir.resolve("META-INF/meshingress/tool-manifest.json");

        McpToolManifestExecutor.export(SampleManifest.class.getName(), output);

        McpToolNativeMetadata metadata = McpToolManifestJson.read(output);
        assertThat(metadata.toolId()).isEqualTo("sample.executor");
        assertThat(metadata.properties()).extracting(ToolProperty::name).containsExactly("sample.executor.enabled");
    }

    @Test
    void rejectsClassesThatAreNotManifestDefinitions() {
        assertThatThrownBy(() -> McpToolManifestExecutor.export(String.class.getName(), tempDir.resolve("manifest.json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not implement McpToolManifestDefinition");
    }

    public static final class SampleManifest implements McpToolManifestDefinition {

        @Override
        public String toolId() {
            return "sample.executor";
        }

        @Override
        public List<ToolProperty> properties() {
            return List.of(ToolProperty.string("sample.executor.enabled").defaultValue("true"));
        }
    }
}
