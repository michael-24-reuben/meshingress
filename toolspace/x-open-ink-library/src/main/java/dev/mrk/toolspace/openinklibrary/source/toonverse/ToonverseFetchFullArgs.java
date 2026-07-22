package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ToonverseFetchFullArgs(
        @McpInputField(value = "name", description = "Exact work name or Toonverse slug.", required = true) String name,
        @McpInputField(value = "limit", description = "Chapter page size from 1 through 500; defaults to 50.", required = false) Integer limit,
        @McpInputField(value = "offset", description = "Zero-based chapter page offset; defaults to 0.", required = false) Integer offset,
        @McpInputField(value = "order", description = "Chapter order: asc or desc; defaults to asc.", required = false) String order
) {
}
