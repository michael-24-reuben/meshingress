package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record YtDlpInspectArgs(
        @McpInputField(value = "url", description = "HTTP or HTTPS media URL to inspect.", required = true)
        String url
) { }
