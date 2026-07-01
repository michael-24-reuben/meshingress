package dev.mrk.toolspace.instagram;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.dispatch.data.RecordContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.toolspace.instagram.instafetch.FetchPath;
import dev.mrk.toolspace.instagram.instafetch.InstaFetch;
import tools.jackson.databind.JsonNode;

@McpTool(
        value = "instagram",
        title = "Instagram Fetch",
        description = "Fetches data from Instagram based on a given URL.",
        defaultFunction = "fetch"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.NETWORK_ACCESS,
        McpToolScope.EXTERNAL_API_READ
})
public class InstaFetchTool {
    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(
            value = "fetch",
            title = "Instagram Fetch",
            description = "Fetches data from Instagram based on a given URL."
    )
    public DispatchExecutionResult fetch(
            @McpFunctionParam(value = "url", description = "The URL of the Instagram post to fetch data from.")
            String url,
            McpCallContext context
    ) {
        if (url == null || url.isBlank()) {
            return DispatchExecutionResult.builder()
                    .error("INSTAGRAM_URL_REQUIRED", "The url parameter is required.")
                    .status("failed")
                    .summary("Instagram fetch failed because no URL was provided.")
                    .build();
        }

        JsonNode response = new InstaFetch(FetchPath.from(url)).submitRequest();
        RecordContent structured = new RecordContent();
        structured.setId(url);
        structured.setTitle("Instagram fetch");
        structured.setRecord(response);

        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.json(response))
                .structuredContent(structured)
                .status("ok")
                .summary("Fetched Instagram data.")
                .build();
    }
}
