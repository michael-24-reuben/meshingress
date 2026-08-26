package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolModuleMetadata;

import java.util.List;

/** Module identity for the local media tool family. */
public final class YtDlpManifest implements McpToolManifestDefinition {
    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "media",
                "yt-dlp",
                "Local media inspection and download",
                "Constrained local media operations through a vendored yt-dlp runtime.",
                List.of(), "", List.of("media", "local", "yt-dlp"), List.of(), null
        );
    }
}
