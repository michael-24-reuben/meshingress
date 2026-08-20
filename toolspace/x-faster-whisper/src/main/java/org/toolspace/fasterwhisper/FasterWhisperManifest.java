package org.toolspace.fasterwhisper;

import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolLink;
import dev.mrk.meshingress.toolmetadata.ToolModuleMetadata;
import dev.mrk.meshingress.toolmetadata.ToolProperty;
import dev.mrk.meshingress.toolmetadata.ToolReadme;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;

import java.util.List;

/** Static external-resource and configuration declaration for the Faster Whisper tool. */
public final class FasterWhisperManifest implements McpToolManifestDefinition {

    static final String FASTER_WHISPER_COMMIT = "ed9a06cd89a93e47838f564998a6c09b655d7f43";

    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "faster-whisper",
                "Faster Whisper",
                "Local audio transcription",
                "Transcribes local audio through an isolated Faster Whisper Python environment.",
                List.of(), "", List.of("audio", "local", "transcription"), List.of(
                        ToolLink.documentation("https://github.com/SYSTRAN/faster-whisper").label("Faster Whisper documentation"),
                        ToolLink.source("https://github.com/SYSTRAN/faster-whisper").label("Faster Whisper source")
                ), null
        );
    }

    @Override
    public List<ToolProperty> properties() {
        return FasterWhisperManifestProperties.manifestProperties();
    }

    @Override
    public List<ToolRequirement> requirements() {
        return List.of(
                ToolRequirement.sourceRepository("https://github.com/SYSTRAN/faster-whisper")
                        .cloneUrl("https://github.com/SYSTRAN/faster-whisper.git")
                        .checkoutRef(FASTER_WHISPER_COMMIT)
                        .description("Pinned Faster Whisper source used to provision the isolated Python environment."),
                ToolRequirement.scope(McpToolScope.FILES_READ),
                ToolRequirement.scope(McpToolScope.PROCESS_EXECUTE)
        );
    }

    @Override
    public ToolReadme readme() {
        return ToolReadme.inline("""
                # Faster Whisper

                Transcribes local audio through an isolated Faster Whisper Python environment.

                Activation requires the pinned Faster Whisper source revision declared in this
                manifest. Meshingress provisions it into its managed vendor workspace; the local
                development `vendor/` checkout and `.venv` are deliberately not published.

                The provisioned Python interpreter must be supplied through
                `meshingress.faster-whisper.python-executable`. Ordinary tool calls only use that
                verified interpreter and require `model.local-files-only=true`; they never install
                dependencies or download models.
                """);
    }
}
