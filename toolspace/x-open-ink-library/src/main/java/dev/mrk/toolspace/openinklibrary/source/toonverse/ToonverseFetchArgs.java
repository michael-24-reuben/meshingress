package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ToonverseFetchArgs(
        @McpInputField(value = "name", description = "Exact work name or Toonverse slug.", required = true)
        String name
) {
}
