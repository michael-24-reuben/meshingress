package dev.mrk.meshingress.toolcatalog;

import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolModuleMetadata;
import dev.mrk.meshingress.toolmetadata.ToolProperty;
import dev.mrk.meshingress.toolmetadata.ToolReadme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolModuleCatalogTests {

    @Test
    void registersAndRemovesARuntimeModuleWithoutARepositoryArtifact() {
        ToolModuleCatalog catalog = new ToolModuleCatalog(List.of());
        ToolModuleId moduleId = new ToolModuleId("opensource:sample:1.0.0");

        catalog.registerRuntimeModule(moduleId, List.of(new SampleManifest()));

        ToolModuleCatalog.Entry entry = catalog.find(moduleId.value()).orElseThrow();
        assertThat(entry.source()).isEqualTo("runtime");
        assertThat(entry.toolId()).startsWith("rt-");
        assertThat(catalog.findByToolId(entry.toolId())).contains(entry);
        assertThat(entry.namespace()).isEqualTo("sample");
        assertThat(entry.manifest().properties()).extracting(ToolProperty::name).containsExactly("sample.token");
        assertThat(entry.readme()).isEqualTo("# Sample module");

        catalog.unregister(moduleId.value());

        assertThat(catalog.find(moduleId.value())).isEmpty();
    }

    private static final class SampleManifest implements McpToolManifestDefinition {
        @Override
        public ToolModuleMetadata metadata() {
            return new ToolModuleMetadata("sample", "Sample", "", "", List.of(), "Apache-2.0", List.of(), List.of(), null);
        }

        @Override
        public List<ToolProperty> properties() {
            return List.of(ToolProperty.secretRef("sample.token"));
        }

        @Override
        public ToolReadme readme() {
            return ToolReadme.inline("# Sample module");
        }
    }
}
