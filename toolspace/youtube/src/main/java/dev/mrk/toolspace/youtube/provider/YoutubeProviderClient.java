package dev.mrk.toolspace.youtube.provider;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import tools.jackson.databind.JsonNode;

/**
 * Future provider adapters implement this boundary after their credentials, scopes, request
 * schemas, error mapping, and policy constraints have each been reviewed.
 */
public interface YoutubeProviderClient {
    String providerId();

    boolean supports(String functionId);

    DispatchExecutionResult execute(String functionId, JsonNode arguments, McpCallContext context);
}
