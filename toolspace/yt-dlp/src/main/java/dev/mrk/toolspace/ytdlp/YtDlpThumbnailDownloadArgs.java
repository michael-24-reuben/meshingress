package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record YtDlpThumbnailDownloadArgs(
        @McpInputField(value = "url", description = "HTTP or HTTPS media URL whose thumbnail will be downloaded.", required = true)
        String url,
        @McpInputField(value = "all", description = "Download every available thumbnail instead of only yt-dlp's selected thumbnail.", required = false)
        Boolean all
) { }
