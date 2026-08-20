package dev.mrk.toolspace.youtube.data;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

import java.util.List;

public record YoutubeResourceArgs(
        @McpInputField(value = "ids", description = "One through 50 YouTube resource IDs.") List<String> ids
) {
}
