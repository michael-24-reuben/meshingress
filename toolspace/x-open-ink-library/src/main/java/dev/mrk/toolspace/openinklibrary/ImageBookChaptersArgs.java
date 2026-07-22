package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ImageBookChaptersArgs(
        @McpInputField(value = "source", description = "Installed image-book source ID.", required = true)
        String source,
        @McpInputField(value = "sourceBookId", description = "Source-native work ID returned by metadata.", required = true)
        String sourceBookId,
        @McpInputField(value = "limit", description = "Optional chapter count limit.", required = false)
        Integer limit,
        @McpInputField(value = "order", description = "Optional chapter order: asc or desc.", required = false)
        String order,
        @McpInputField(value = "authToken", description = "Optional execution-scoped bearer token.", required = false)
        String authToken
) {
}
