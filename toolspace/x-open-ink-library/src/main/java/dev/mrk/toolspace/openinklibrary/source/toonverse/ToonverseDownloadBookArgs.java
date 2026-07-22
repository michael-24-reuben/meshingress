package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ToonverseDownloadBookArgs(
        @McpInputField(value = "name", description = "Exact work name or Toonverse slug.", required = true) String name,
        @McpInputField(value = "minChapterNumber", description = "First Toonverse chapter number to download.", required = true) Integer minChapterNumber,
        @McpInputField(value = "maxChapterNumber", description = "Last Toonverse chapter number to download, inclusively.", required = true) Integer maxChapterNumber,
        @McpInputField(value = "ttlSeconds", description = "Optional storage lifetime in seconds; the server applies its configured maximum.") Integer ttlSeconds,
        @McpInputField(value = "maxRequests", description = "Optional total GET request budget for the published workspace; the server applies its configured maximum.") Integer maxRequests
) { }
