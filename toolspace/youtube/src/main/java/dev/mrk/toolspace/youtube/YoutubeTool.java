package dev.mrk.toolspace.youtube;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.dispatch.data.RecordContent;
import dev.mrk.toolspace.youtube.data.YoutubeDataApiClient;
import dev.mrk.toolspace.youtube.data.YoutubeDataApiException;
import dev.mrk.toolspace.youtube.data.YoutubeApiResponseContent;
import dev.mrk.toolspace.youtube.data.YoutubeResourceArgs;
import dev.mrk.toolspace.youtube.data.YoutubeSearchArgs;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@McpTool(
        value = "data",
        title = "YouTube Data API",
        description = "Perform selected public YouTube Data API reads."
)
@McpToolScopes(McpToolScope.NETWORK_OUTBOUND)
public final class YoutubeTool {
    private final YoutubeDataApiClient dataApiClient;

    YoutubeTool(YoutubeDataApiClient dataApiClient) {
        this.dataApiClient = dataApiClient;
    }

    @McpFunction(value = "search", title = "Search YouTube", description = "Search public YouTube videos, channels, or playlists through the official Data API.", outputTypes = RecordContent.class)
    public DispatchExecutionResult search(YoutubeSearchArgs arguments, McpCallContext context) {
        if (arguments == null || isBlank(arguments.query())) {
            return invalid("query is required.");
        }
        Integer maxResults = normalizeMaxResults(arguments.maxResults());
        if (maxResults == null) {
            return invalid("maxResults must be from 1 through 50.");
        }
        return call("search", () -> dataApiClient.search(Map.of(
                "part", "snippet",
                "q", arguments.query().strip(),
                "type", blankOr(arguments.type(), "video"),
                "maxResults", Integer.toString(maxResults),
                "pageToken", blankOr(arguments.pageToken(), ""),
                "channelId", blankOr(arguments.channelId(), ""),
                "order", blankOr(arguments.order(), "relevance")
        )), "YouTube search completed.");
    }

    @McpFunction(value = "videos-get", title = "Get videos", description = "Get public video metadata through the official YouTube Data API.", outputTypes = YoutubeApiResponseContent.class)
    public DispatchExecutionResult videos(YoutubeResourceArgs arguments, McpCallContext context) {
        return resources(arguments, "videos", "snippet,contentDetails,statistics,status,liveStreamingDetails", dataApiClient::videos, "YouTube video lookup completed.");
    }

    @McpFunction(value = "channels-get", title = "Get channels", description = "Get public channel metadata through the official YouTube Data API.", outputTypes = YoutubeApiResponseContent.class)
    public DispatchExecutionResult channels(YoutubeResourceArgs arguments, McpCallContext context) {
        return resources(arguments, "channels", "snippet,contentDetails,statistics,brandingSettings,status", dataApiClient::channels, "YouTube channel lookup completed.");
    }

    @McpFunction(value = "playlists-get", title = "Get playlists", description = "Get public playlist metadata through the official YouTube Data API.", outputTypes = YoutubeApiResponseContent.class)
    public DispatchExecutionResult playlists(YoutubeResourceArgs arguments, McpCallContext context) {
        return resources(arguments, "playlists", "snippet,contentDetails,status", dataApiClient::playlists, "YouTube playlist lookup completed.");
    }

    private DispatchExecutionResult resources(
            YoutubeResourceArgs arguments,
            String resource,
            String part,
            java.util.function.Function<Map<String, String>, JsonNode> operation,
            String summary
    ) {
        if (arguments == null || arguments.ids() == null || arguments.ids().isEmpty() || arguments.ids().size() > 50
                || arguments.ids().stream().anyMatch(YoutubeTool::isBlank)) {
            return invalid("ids must contain from 1 through 50 nonblank resource IDs.");
        }
        return call(resource, () -> operation.apply(Map.of(
                "part", part,
                "id", String.join(",", arguments.ids().stream().map(String::strip).toList())
        )), summary);
    }

    private DispatchExecutionResult call(String resource, java.util.function.Supplier<JsonNode> operation, String summary) {
        try {
            JsonNode response = operation.get();
            return DispatchExecutionResult.builder()
                    .object(response)
                    .structuredContent(new YoutubeApiResponseContent(resource, response))
                    .status("completed")
                    .summary(summary)
                    .build();
        } catch (YoutubeDataApiException exception) {
            return DispatchExecutionResult.builder()
                    .error(exception.code(), exception.getMessage())
                    .status("failed")
                    .summary("YouTube Data API request failed.")
                    .build();
        }
    }

    private static DispatchExecutionResult invalid(String message) {
        return DispatchExecutionResult.builder().error("INVALID_ARGUMENTS", message).status("failed").summary("YouTube Data API request is invalid.").build();
    }

    private static Integer normalizeMaxResults(Integer value) {
        if (value == null) {
            return 10;
        }
        return value >= 1 && value <= 50 ? value : null;
    }

    private static String blankOr(String value, String fallback) {
        return isBlank(value) ? fallback : value.strip();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
