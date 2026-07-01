package dev.mrk.toolspace.cobalt;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.dispatch.tool.ToolResultContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.Objects;

@McpTool(
        value = "cobalt",
        title = "Cobalt",
        description = "Call a configured imputnet/cobalt API instance for public media processing.",
        defaultFunction = "process"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.HTTP_CLIENT,
        McpToolScope.EXTERNAL_API_READ,
        McpToolScope.EXTERNAL_API_WRITE
})
public class CobaltTool {

    private final CobaltClient client;
    private final ObjectMapper objectMapper;

    public CobaltTool(CobaltClient client, ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @McpConfigureMapping(timeoutMs = 15_000, audit = true)
    @McpFunction(
            value = "info",
            title = "Cobalt Info",
            description = "Read version, supported services, and instance metadata from the configured Cobalt API."
    )
    @McpToolScopes({
            McpToolScope.HEALTH_CHECK,
            McpToolScope.EXTERNAL_API_READ
    })
    public DispatchExecutionResult info(CobaltInfoArgs arguments, McpCallContext context) {
        return run("Cobalt instance info loaded.", false, client::info);
    }

    @McpConfigureMapping(timeoutMs = 120_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "process",
            title = "Process Media URL",
            description = "Submit a public media URL to Cobalt's POST / processing endpoint and return redirect, tunnel, picker, local-processing, or error details."
    )
    @McpToolScopes({
            McpToolScope.HTTP_CLIENT,
            McpToolScope.EXTERNAL_API_WRITE
    })
    public DispatchExecutionResult process(CobaltProcessArgs arguments, McpCallContext context) {
        return run("Cobalt media request processed.", true, () -> client.process(arguments));
    }

    private DispatchExecutionResult run(String summary, boolean mutating, CobaltCall call) {
        try {
            JsonNode response = call.execute();
            ToolResultContent structured = base(mutating);
            structured.setResponse(response);
            structured.setUpstreamStatus(status(response));
            structured.setBaseUrl(client.baseUri().toString());

            if ("error".equals(status(response))) {
                return DispatchExecutionResult.builder()
                        .appendContent(ResultContent.json(response))
                        .structuredContent(structured)
                        .error("COBALT_PROCESSING_ERROR", cobaltErrorMessage(response))
                        .status("failed")
                        .summary("Cobalt returned an error response.")
                        .build();
            }

            return DispatchExecutionResult.builder()
                    .appendContent(ResultContent.json(response))
                    .structuredContent(structured)
                    .status("ok")
                    .summary(summary)
                    .build();
        } catch (Exception exception) {
            return failure(exception, mutating);
        }
    }

    private DispatchExecutionResult failure(Exception exception, boolean mutating) {
        ToolResultContent structured = base(mutating);
        structured.setOk(false);
        structured.setExceptionType(exception.getClass().getName());
        structured.setMessage(exception.getMessage() == null ? "" : exception.getMessage());
        structured.setBaseUrl(client.baseUri().toString());
        ObjectNode details = objectMapper.createObjectNode();
        details.put("upstreamPath", "toolspace/cobalt/upstream/cobalt");
        structured.setDetails(details);

        if (exception instanceof CobaltHttpException httpException) {
            structured.setHttp(httpException.response());
        }

        return DispatchExecutionResult.builder()
                .structuredContent(structured)
                .error(errorCode(exception), CobaltClient.userMessage(exception))
                .status("failed")
                .summary("Cobalt request failed.")
                .build();
    }

    private ToolResultContent base(boolean mutating) {
        ToolResultContent structured = new ToolResultContent();
        structured.setTool("cobalt");
        structured.setOk(true);
        structured.setMutating(mutating);
        structured.setReceivedAt(Instant.now().toString());
        structured.setUpstreamRepository("https://github.com/imputnet/cobalt");
        structured.setUpstreamCommit("a636575b09de1fc55d9b8cd98cac88f5f2f16b42");
        return structured;
    }

    private static String status(JsonNode response) {
        JsonNode status = response == null ? null : response.get("status");
        return status == null || !status.isTextual() ? "" : status.asText();
    }

    private static String cobaltErrorMessage(JsonNode response) {
        JsonNode error = response == null ? null : response.get("error");
        if (error != null && error.get("code") != null && error.get("code").isTextual()) {
            return error.get("code").asText();
        }
        return "Cobalt returned an error response.";
    }

    private static String errorCode(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return "COBALT_INVALID_ARGUMENTS";
        }
        if (exception instanceof CobaltHttpException) {
            return "COBALT_HTTP_ERROR";
        }
        if (exception instanceof ConnectException || exception.getCause() instanceof ConnectException) {
            return "COBALT_UNAVAILABLE";
        }
        if (exception instanceof HttpTimeoutException) {
            return "COBALT_TIMEOUT";
        }
        if (exception instanceof IOException) {
            return "COBALT_IO_ERROR";
        }
        return "COBALT_REQUEST_FAILED";
    }

    @FunctionalInterface
    private interface CobaltCall {
        JsonNode execute() throws Exception;
    }
}
