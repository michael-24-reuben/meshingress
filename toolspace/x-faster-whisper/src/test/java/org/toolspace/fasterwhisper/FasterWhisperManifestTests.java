package org.toolspace.fasterwhisper;

import dev.mrk.meshingress.toolmetadata.McpToolManifestJson;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadata;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FasterWhisperManifestTests {

    @Test
    void declaresPinnedFasterWhisperSourceAndRuntimeConfiguration() {
        FasterWhisperManifest manifest = new FasterWhisperManifest();

        ToolRequirement source = manifest.requirements().stream()
                .filter(requirement -> "sourceRepository".equals(requirement.kind()))
                .findFirst()
                .orElseThrow();

        assertThat(manifest.toolId()).isEqualTo("fasterwhisper");
        assertThat(source.canonicalIdentity()).isEqualTo("github.com/SYSTRAN/faster-whisper");
        assertThat(source.cloneUrl()).isEqualTo("https://github.com/SYSTRAN/faster-whisper.git");
        assertThat(source.checkoutRef()).isEqualTo(FasterWhisperManifest.FASTER_WHISPER_COMMIT);
        assertThat(manifest.properties())
                .extracting(property -> property.name())
                .contains(
                        "meshingress.faster-whisper.python-executable",
                        "meshingress.faster-whisper.model.local-files-only",
                        "meshingress.faster-whisper.generation.beam-size"
                );
    }

    @Test
    void packagesTheStaticManifestUsedByRepositoryAssessment() throws Exception {
        Path staticManifest = Path.of("target/classes/META-INF/meshingress/tool-manifest.json");

        assertThat(Files.isRegularFile(staticManifest)).isTrue();

        McpToolNativeMetadata metadata = McpToolManifestJson.read(staticManifest);
        ToolRequirement source = metadata.requirements().stream()
                .filter(requirement -> "sourceRepository".equals(requirement.kind()))
                .findFirst()
                .orElseThrow();
        assertThat(metadata.toolId()).isEqualTo("fasterwhisper");
        assertThat(source.canonicalIdentity()).isEqualTo("github.com/SYSTRAN/faster-whisper");
        assertThat(source.checkoutRef()).isEqualTo(FasterWhisperManifest.FASTER_WHISPER_COMMIT);
    }
}
