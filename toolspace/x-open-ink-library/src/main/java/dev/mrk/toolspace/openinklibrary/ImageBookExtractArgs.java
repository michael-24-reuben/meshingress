package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ImageBookExtractArgs(
        @McpInputField(value = "source", description = "Installed image-book source ID.", required = true)
        String source,
        @McpInputField(value = "slug", description = "Source-specific work slug.", required = true)
        String slug,
        @McpInputField(value = "limit", description = "Optional chapter count limit.", required = false)
        Integer limit,
        @McpInputField(value = "order", description = "Optional chapter order: asc or desc.", required = false)
        String order,
        @McpInputField(value = "authToken", description = "Optional execution-scoped bearer token.", required = false)
        String authToken
) {
}
