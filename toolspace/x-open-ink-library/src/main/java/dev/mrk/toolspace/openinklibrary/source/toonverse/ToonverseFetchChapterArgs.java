package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ToonverseFetchChapterArgs(
        @McpInputField(value = "name", description = "Exact work name or Toonverse slug.", required = true) String name,
        @McpInputField(value = "chapterNumber", description = "Zero-based Toonverse chapter number.", required = true) Integer chapterNumber
) {
}
