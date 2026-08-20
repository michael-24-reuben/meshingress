package dev.mrk.toolspace.youtube.data;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record YoutubeSearchArgs(
        @McpInputField(value = "query", description = "Search terms passed to the YouTube Data API q parameter.") String query,
        @McpInputField(value = "type", description = "Optional result type: video, channel, playlist, or a comma-separated combination. Defaults to video.", required = false) String type,
        @McpInputField(value = "maxResults", description = "Optional result count from 1 through 50. Defaults to 10.", required = false) Integer maxResults,
        @McpInputField(value = "pageToken", description = "Optional page token from a previous response.", required = false) String pageToken,
        @McpInputField(value = "channelId", description = "Optional channel ID that restricts the search.", required = false) String channelId,
        @McpInputField(value = "order", description = "Optional API sort order such as relevance, date, rating, title, or viewCount.", required = false) String order
) {
}
