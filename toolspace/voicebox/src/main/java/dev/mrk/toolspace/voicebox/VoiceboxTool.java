package dev.mrk.toolspace.voicebox;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
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
        value = "voicebox",
        title = "Voicebox",
        description = "Call a local jamiepine/voicebox backend for speech, transcription, profiles, and health.",
        defaultFunction = "speak"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.HTTP_CLIENT,
        McpToolScope.EXTERNAL_API_READ,
        McpToolScope.EXTERNAL_API_WRITE,
        McpToolScope.FILES_READ
})
public class VoiceboxTool {

    private final VoiceboxClient client;
    private final ObjectMapper objectMapper;

    public VoiceboxTool(VoiceboxClient client, ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @McpConfigureMapping(timeoutMs = 15_000, audit = true)
    @McpFunction(
            value = "health",
            title = "Voicebox Health",
            description = "Read health, backend, GPU, and model status from the local Voicebox server."
    )
    @McpToolScopes(McpToolScope.HEALTH_CHECK)
    public DispatchExecutionResult health(VoiceboxHealthArgs arguments, McpCallContext context) {
        return runRead("Voicebox health loaded.", client::health);
    }

    @McpConfigureMapping(timeoutMs = 20_000, audit = true)
    @McpFunction(
            value = "list_profiles",
            title = "List Voicebox Profiles",
            description = "List available cloned and preset Voicebox profiles."
    )
    @McpToolScopes(McpToolScope.EXTERNAL_API_READ)
    public DispatchExecutionResult listProfiles(VoiceboxListProfilesArgs arguments, McpCallContext context) {
        return runRead("Voicebox profiles loaded.", () -> {
            ObjectNode structured = objectMapper.createObjectNode();
            structured.set("profiles", client.listProfiles());
            return structured;
        });
    }

    @McpConfigureMapping(timeoutMs = 120_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "speak",
            title = "Speak With Voicebox",
            description = "Submit text to Voicebox /speak and return the generation id and status for polling."
    )
    @McpToolScopes({
            McpToolScope.EXTERNAL_API_WRITE,
            McpToolScope.NOTIFICATIONS_SEND
    })
    public DispatchExecutionResult speak(VoiceboxSpeakArgs arguments, McpCallContext context) {
        return runWrite("Voicebox speech generation started.", () -> client.speak(arguments));
    }

    @McpConfigureMapping(timeoutMs = 45_000, audit = true)
    @McpFunction(
            value = "generation_status",
            title = "Voicebox Generation Status",
            description = "Fetch Voicebox generation status for an id returned by voicebox.speak."
    )
    @McpToolScopes(McpToolScope.EXTERNAL_API_READ)
    public DispatchExecutionResult generationStatus(VoiceboxGenerationStatusArgs arguments, McpCallContext context) {
        return runRead("Voicebox generation status loaded.", () -> client.generationStatus(arguments));
    }

    @McpConfigureMapping(timeoutMs = 600_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "transcribe_file",
            title = "Transcribe Local Audio With Voicebox",
            description = "Upload a local audio file to Voicebox /transcribe and return the transcript."
    )
    @McpToolScopes({
            McpToolScope.FILES_READ,
            McpToolScope.EXTERNAL_API_WRITE
    })
    public DispatchExecutionResult transcribeFile(VoiceboxTranscribeFileArgs arguments, McpCallContext context) {
        return runWrite("Voicebox transcription completed.", () -> client.transcribeFile(arguments));
    }

    private DispatchExecutionResult runRead(String summary, VoiceboxCall call) {
        return run(summary, false, call);
    }

    private DispatchExecutionResult runWrite(String summary, VoiceboxCall call) {
        return run(summary, true, call);
    }

    private DispatchExecutionResult run(String summary, boolean mutating, VoiceboxCall call) {
        try {
            JsonNode response = call.execute();
            ObjectNode structured = objectMapper.createObjectNode();
            structured.put("ok", true);
            structured.put("mutating", mutating);
            structured.put("receivedAt", Instant.now().toString());
            structured.set("response", response);

            return DispatchExecutionResult.builder()
                    .appendContent(ResultContent.json(response))
                    .structuredContent(structured)
                    .status("ok")
                    .summary(summary)
                    .build();
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    private DispatchExecutionResult failure(Exception exception) {
        ObjectNode structured = objectMapper.createObjectNode();
        structured.put("ok", false);
        structured.put("receivedAt", Instant.now().toString());
        structured.put("exceptionType", exception.getClass().getName());
        structured.put("message", exception.getMessage() == null ? "" : exception.getMessage());

        if (exception instanceof VoiceboxHttpException httpException) {
            structured.set("http", httpException.response());
        }

        String code = errorCode(exception);
        return DispatchExecutionResult.builder()
                .structuredContent(structured)
                .error(code, userMessage(exception))
                .status("failed")
                .summary("Voicebox request failed.")
                .build();
    }

    private String errorCode(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return "VOICEBOX_INVALID_ARGUMENTS";
        }
        if (exception instanceof VoiceboxHttpException) {
            return "VOICEBOX_HTTP_ERROR";
        }
        if (exception instanceof ConnectException || exception.getCause() instanceof ConnectException) {
            return "VOICEBOX_UNAVAILABLE";
        }
        if (exception instanceof HttpTimeoutException) {
            return "VOICEBOX_TIMEOUT";
        }
        if (exception instanceof IOException) {
            return "VOICEBOX_IO_ERROR";
        }
        return "VOICEBOX_REQUEST_FAILED";
    }

    private String userMessage(Exception exception) {
        if (exception instanceof ConnectException || exception.getCause() instanceof ConnectException) {
            return "Could not connect to Voicebox. Start it with `python -m backend.main --host 127.0.0.1 --port 17493` or set meshingress.voicebox.base-url.";
        }
        return exception.getMessage() == null ? "Voicebox request failed." : exception.getMessage();
    }

    @FunctionalInterface
    private interface VoiceboxCall {
        JsonNode execute() throws Exception;
    }
}
