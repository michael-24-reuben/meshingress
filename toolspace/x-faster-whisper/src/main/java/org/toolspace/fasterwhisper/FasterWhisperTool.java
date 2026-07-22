package org.toolspace.fasterwhisper;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@McpTool(value = "fasterwhisper", title = "Faster Whisper", description = "Transcribe a local audio file with a provisioned faster-whisper environment.")
@McpToolScopes({McpToolScope.FILES_READ, McpToolScope.PROCESS_EXECUTE})
@McpToolMapping("tools")
public final class FasterWhisperTool {
    private final ObjectMapper objectMapper;
    private final FasterWhisperManifestProperties properties;
    private final FasterWhisperBridgeScript bridgeScript;

    FasterWhisperTool(ObjectMapper objectMapper, FasterWhisperManifestProperties properties, FasterWhisperBridgeScript bridgeScript) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.bridgeScript = bridgeScript;
    }

    @McpConfigureMapping(/*timeoutMs = 900_000*/ audit = true)
    @McpFunction(value = "transcribe", title = "Transcribe audio", description = "Runs the verified faster-whisper Python bridge without installing dependencies or downloading models.")
    public DispatchExecutionResult transcribe(FasterWhisperTranscribeArgs arguments, McpCallContext context) {
        try {
            validate(arguments);
            JsonNode response = invoke(arguments);
            boolean error = response.path("ok").isBoolean() && !response.path("ok").booleanValue();
            ToolResultContent structured = new ToolResultContent();
            structured.setTool("faster-whisper");
            structured.setOperation("transcribe");
            structured.setStatus(error ? "failed" : "completed");
            structured.setOk(!error);
            structured.setMutating(false);
            structured.setResponse(response);
            structured.setMessage(response.path("error").asString(""));
            return DispatchExecutionResult.builder()
                    .object(response)
                    .structuredContent(structured)
                    .error(error)
                    .status(error ? "failed" : "completed")
                    .summary(error ? "faster-whisper bridge returned an error." : "Audio transcription completed.")
                    .build();
        } catch (Exception exception) {
            return DispatchExecutionResult.builder()
                    .error("FASTER_WHISPER_UNAVAILABLE", exception.getMessage() == null ? "faster-whisper failed" : exception.getMessage())
                    .status("failed")
                    .summary("faster-whisper could not start; provision its local Python environment and model first.")
                    .build();
        }
    }

    private JsonNode invoke(FasterWhisperTranscribeArgs arguments) throws Exception {
        Path interpreter = Path.of(properties.pythonExecutable()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(interpreter)) {
            throw new IllegalStateException("Verified Python interpreter is unavailable: " + interpreter);
        }
        Path script = bridgeScript.resolve(properties);
        Process process = new ProcessBuilder(List.of(interpreter.toString(), script.toString(), "--json-stdin"))
                .redirectErrorStream(false)
                .start();
        ObjectNode payload = payload(arguments);
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(objectMapper.writeValueAsBytes(payload));
        }
        CompletableFuture<String> stdout = readAsync(process.getInputStream());
        CompletableFuture<String> stderr = readAsync(process.getErrorStream());
        if (!process.waitFor(properties.timeoutMs(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("faster-whisper bridge timed out after " + properties.timeoutMs() + " ms");
        }
        String output = stdout.get(10, TimeUnit.SECONDS);
        String errors = stderr.get(10, TimeUnit.SECONDS);
        if (process.exitValue() != 0) {
            throw new IllegalStateException("faster-whisper bridge exited with " + process.exitValue() + ": " + truncate(errors));
        }
        return objectMapper.readTree(output);
    }

    private ObjectNode payload(FasterWhisperTranscribeArgs arguments) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("audioPath", arguments.audioPath());
        ObjectNode model = objectMapper.valueToTree(properties.model());
        if (!model.path("localFilesOnly").asBoolean(true)) {
            throw new IllegalStateException("Model downloads are provisioning work; meshingress.faster-whisper.model.local-files-only must remain true for tool calls.");
        }
        payload.set("model", model);
        ObjectNode generation = objectMapper.valueToTree(properties.generation());
        ObjectNode callOverrides = objectMapper.valueToTree(arguments);
        callOverrides.properties().forEach(entry -> {
            if (!entry.getKey().equals("audioPath") && !entry.getValue().isNull()) {
                generation.set(entry.getKey(), entry.getValue());
            }
        });
        payload.set("generation", generation);
        return payload;
    }

    private static CompletableFuture<String> readAsync(java.io.InputStream input) {
        return CompletableFuture.supplyAsync(() -> {
            try (input) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                return "";
            }
        });
    }

    private static String truncate(String value) {
        return value.length() <= 4_000 ? value : value.substring(0, 4_000);
    }

    private static void validate(FasterWhisperTranscribeArgs arguments) {
        if (arguments == null || arguments.audioPath() == null || arguments.audioPath().isBlank()) {
            throw new IllegalArgumentException("audioPath must not be blank");
        }
        Path audio = Path.of(arguments.audioPath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(audio)) {
            throw new IllegalArgumentException("audioPath is not a readable regular file: " + audio);
        }
    }
}
