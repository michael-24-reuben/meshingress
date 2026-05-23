package dev.mrk.toolspace.powershellcli;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@McpTool(
        value = "cli.powershell",
        title = "PowerShell CLI",
        description = "Execute a received PowerShell script and append every execution track to the result.",
        defaultFunction = "execute"
)
@McpToolScopes({
        McpToolScope.SHELL_EXECUTE,
        McpToolScope.FILES_WRITE
})
@McpToolMapping("tools")
public class PowerShellCliTool {

    private final ObjectMapper objectMapper;

    public PowerShellCliTool(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @McpConfigureMapping(
            timeoutMs = 120_000,
            audit = true,
            debugTrace = true
    )
    @McpFunction(
            value = "execute",
            title = "Execute PowerShell Script",
            description = "Runs a PowerShell script and returns ordered execution tracks, stdout, stderr, exit code, duration, and status."
    )
    public DispatchExecutionResult execute(PowerShellExecuteArgs arguments, McpCallContext context) {
        DispatchExecutionResult.Builder dispatch = DispatchExecutionResult.builder();
        ArrayNode tracks = objectMapper.createArrayNode();
        AtomicLong sequence = new AtomicLong(0);

        Instant startedAt = Instant.now();
        Path scriptFile = null;
        Process process = null;

        try {
            validateArguments(arguments);

            long timeoutMs = arguments.normalizedTimeoutMs();
            String executable = arguments.normalizedExecutable();
            Path workingDirectory = resolveWorkingDirectory(arguments.workingDirectory());

            scriptFile = writeTemporaryScript(arguments.script());

            appendTrack(dispatch, tracks, sequence, "process.start", "system",
                    objectMapper.createObjectNode()
                            .put("executable", executable)
                            .put("scriptFile", scriptFile.toString())
                            .put("workingDirectory", workingDirectory.toString())
                            .put("timeoutMs", timeoutMs));

            List<String> command = buildCommand(executable, scriptFile, arguments.arguments());
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(false);

            Map<String, String> environment = arguments.environment();
            if (environment != null && !environment.isEmpty()) {
                builder.environment().putAll(environment);
                appendTrack(dispatch, tracks, sequence, "environment.applied", "system",
                        objectMapper.createObjectNode().put("count", environment.size()));
            }

            process = builder.start();

            Process runningProcess = process;
            CompletableFuture<Void> stdoutPump = pumpStream(
                    runningProcess.getInputStream(),
                    "stdout",
                    dispatch,
                    tracks,
                    sequence
            );
            CompletableFuture<Void> stderrPump = pumpStream(
                    runningProcess.getErrorStream(),
                    "stderr",
                    dispatch,
                    tracks,
                    sequence
            );

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                appendTrack(dispatch, tracks, sequence, "process.timeout", "system",
                        objectMapper.createObjectNode()
                                .put("timeoutMs", timeoutMs)
                                .put("action", "destroyForcibly"));

                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }

            CompletableFuture.allOf(stdoutPump, stderrPump).get(5, TimeUnit.SECONDS);

            int exitCode = process.exitValue();
            Instant finishedAt = Instant.now();
            boolean timedOut = !finished;
            boolean isError = timedOut || exitCode != 0;

            appendTrack(dispatch, tracks, sequence, "process.exit", "system",
                    objectMapper.createObjectNode()
                            .put("exitCode", exitCode)
                            .put("timedOut", timedOut)
                            .put("durationMs", Duration.between(startedAt, finishedAt).toMillis()));

            ObjectNode structured = objectMapper.createObjectNode();
            structured.put("status", isError ? "failed" : "completed");
            structured.put("exitCode", exitCode);
            structured.put("timedOut", timedOut);
            structured.put("startedAt", startedAt.toString());
            structured.put("finishedAt", finishedAt.toString());
            structured.put("durationMs", Duration.between(startedAt, finishedAt).toMillis());
            structured.put("trackCount", tracks.size());
            structured.set("tracks", tracks);

            if (arguments.shouldIncludeScriptInStructuredContent()) {
                structured.put("script", arguments.script());
            }

            return dispatch
                    .structuredContent(structured)
                    .error(isError)
                    .status(isError ? "failed" : "completed")
                    .summary(isError
                            ? "PowerShell script failed. Check appended stderr/process tracks."
                            : "PowerShell script completed successfully.")
                    .build();

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            appendException(dispatch, tracks, sequence, "interrupted", interrupted);
            return dispatch
                    .structuredContent(failureStructured(startedAt, tracks, "interrupted", interrupted.getMessage()))
                    .error("POWERSHELL_INTERRUPTED", "PowerShell execution was interrupted.")
                    .status("interrupted")
                    .summary("PowerShell execution was interrupted.")
                    .build();

        } catch (Exception exception) {
            appendException(dispatch, tracks, sequence, "exception", exception);
            return dispatch
                    .structuredContent(failureStructured(startedAt, tracks, "exception", exception.getMessage()))
                    .error("POWERSHELL_EXECUTION_FAILED", exception.getMessage())
                    .status("failed")
                    .summary("PowerShell execution failed before a normal process exit.")
                    .build();

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            if (scriptFile != null) {
                try {
                    Files.deleteIfExists(scriptFile);
                } catch (IOException ignored) {
                    // Do not fail the tool result because temporary cleanup failed.
                }
            }
        }
    }

    private void validateArguments(PowerShellExecuteArgs arguments) {
        if (arguments == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }
        if (arguments.script() == null || arguments.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
    }

    private Path resolveWorkingDirectory(String value) {
        if (value == null || value.isBlank()) {
            return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }

        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("workingDirectory does not exist: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("workingDirectory is not a directory: " + path);
        }
        return path;
    }

    private Path writeTemporaryScript(String script) throws IOException {
        Path file = Files.createTempFile("meshingress-powershell-", ".ps1");

        // Write a UTF-8 BOM for Windows PowerShell compatibility, then the script body.
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = script.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(body, 0, bytes, bom.length, body.length);
        Files.write(file, bytes);

        return file;
    }

    private List<String> buildCommand(String executable, Path scriptFile, List<String> scriptArguments) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-NoLogo");
        command.add("-NoProfile");
        command.add("-NonInteractive");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(scriptFile.toString());

        if (scriptArguments != null) {
            command.addAll(scriptArguments);
        }

        return command;
    }

    private CompletableFuture<Void> pumpStream(
            InputStream stream,
            String streamName,
            DispatchExecutionResult.Builder dispatch,
            ArrayNode tracks,
            AtomicLong sequence
    ) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ObjectNode payload = objectMapper.createObjectNode()
                            .put("text", line);
                    appendTrack(dispatch, tracks, sequence, "stream.line", streamName, payload);
                }
            } catch (IOException exception) {
                ObjectNode payload = objectMapper.createObjectNode()
                        .put("message", exception.getMessage());
                appendTrack(dispatch, tracks, sequence, "stream.read_error", streamName, payload);
            }
        });
    }

    private void appendException(
            DispatchExecutionResult.Builder dispatch,
            ArrayNode tracks,
            AtomicLong sequence,
            String type,
            Exception exception
    ) {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("exceptionType", exception.getClass().getName())
                .put("message", exception.getMessage() == null ? "" : exception.getMessage());
        appendTrack(dispatch, tracks, sequence, type, "system", payload);
    }

    private ObjectNode failureStructured(
            Instant startedAt,
            ArrayNode tracks,
            String status,
            String message
    ) {
        Instant finishedAt = Instant.now();

        ObjectNode structured = objectMapper.createObjectNode();
        structured.put("status", status);
        structured.put("message", message == null ? "" : message);
        structured.put("startedAt", startedAt.toString());
        structured.put("finishedAt", finishedAt.toString());
        structured.put("durationMs", Duration.between(startedAt, finishedAt).toMillis());
        structured.put("trackCount", tracks.size());
        structured.set("tracks", tracks);
        return structured;
    }

    private synchronized void appendTrack(
            DispatchExecutionResult.Builder dispatch,
            ArrayNode tracks,
            AtomicLong sequence,
            String type,
            String stream,
            ObjectNode payload
    ) {
        ObjectNode track = objectMapper.createObjectNode();
        track.put("id", UUID.randomUUID().toString());
        track.put("seq", sequence.incrementAndGet());
        track.put("at", Instant.now().toString());
        track.put("type", type);
        track.put("stream", stream);
        track.set("payload", payload);

        tracks.add(track);
        dispatch.appendContent(ResultContent.object(track.deepCopy()));
    }
}
