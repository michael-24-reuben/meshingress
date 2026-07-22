package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ToonverseFetchChaptersArgs(
        @McpInputField(value = "name", description = "Exact work name or Toonverse slug.", required = true) String name,
        @McpInputField(value = "minChapterNumber", description = "Zero-based Toonverse chapter number.", required = true) Integer minChapterNumber,
        @McpInputField(value = "maxChapterNumber", description = "Zero-based Toonverse chapter number.", required = true) Integer maxChapterNumber
) {
}
