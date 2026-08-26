package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record YtDlpDownloadArgs(
        @McpInputField(value = "url", description = "HTTP or HTTPS media URL to download.", required = true)
        String url,
        @McpInputField(value = "format", description = "Optional yt-dlp format selector; shell syntax is not accepted.", required = false)
        String format
) { }
