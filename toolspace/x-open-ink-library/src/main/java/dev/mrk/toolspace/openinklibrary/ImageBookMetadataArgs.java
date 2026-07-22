package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ImageBookMetadataArgs(
        @McpInputField(value = "source", description = "Installed image-book source ID.", required = true)
        String source,

        @McpInputField(value = "slug", description = "Source-specific work slug.", required = true)
        String slug,

        @McpInputField(
                value = "authToken",
                description = "Optional execution-scoped bearer token. It is never persisted or returned.",
                required = false
        )
        String authToken
) {
}
