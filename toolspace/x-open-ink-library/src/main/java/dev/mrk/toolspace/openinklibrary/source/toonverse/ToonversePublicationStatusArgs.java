package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

/** Identifies the workspace returned by toonverse.download-book. */
public record ToonversePublicationStatusArgs(
        @McpInputField(value = "sessionId", description = "Workspace session ID returned by download-book.", required = true) String sessionId,
        @McpInputField(value = "requestId", description = "Workspace request ID returned by download-book.", required = true) String requestId
) { }
