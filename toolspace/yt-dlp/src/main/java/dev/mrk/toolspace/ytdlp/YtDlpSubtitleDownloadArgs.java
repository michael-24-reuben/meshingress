package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record YtDlpSubtitleDownloadArgs(
        @McpInputField(value = "url", description = "HTTP or HTTPS media URL whose subtitles will be downloaded.", required = true)
        String url,
        @McpInputField(value = "languages", description = "Comma-separated yt-dlp subtitle language selector, such as en,en-US or all,-live_chat.", required = true)
        String languages,
        @McpInputField(value = "format", description = "Optional subtitle format preference, such as srt or ass/srt/best.", required = false)
        String format,
        @McpInputField(value = "includeAutomaticCaptions", description = "Also download automatically generated captions when available.", required = false)
        Boolean includeAutomaticCaptions
) { }
