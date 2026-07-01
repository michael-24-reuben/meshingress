package dev.mrk.toolspace.videoloop;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.dispatch.process.ProcessExecutionContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@McpTool(
        value = "video.loop",
        title = "Video Loop",
        description = "Find visually similar loop endpoints and optionally export a seamless loop clip using the vendored video-loop-finder reference.",
        defaultFunction = "find"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.FILES_READ,
        McpToolScope.PROCESS_EXECUTE
})
public class VideoLoopTool {

    private static final Duration TIMEOUT = Duration.ofMinutes(3);
    private static final Path DEFAULT_VENDOR_SCRIPT = Path.of(
            "Meshingress", "vendor", "media", "video-loop-finder", "video_loop_finder.py"
    );

    private final ObjectMapper objectMapper;

    public VideoLoopTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @McpFunction(
            value = "find",
            title = "Find Loop Endpoint",
            description = "Given a selected start frame, find a matching later frame and optionally write a clipped loop file."
    )
    @McpConfigureMapping(timeoutMs = 180_000, audit = true)
    @McpToolScopes({
            McpToolScope.FILES_METADATA_READ,
            McpToolScope.FILES_WRITE
    })
    public DispatchExecutionResult find(VideoLoopFindArgs args, McpCallContext context) {
        try {
            validate(args);

            Path videoPath = Path.of(args.videoPath()).normalize();
            Path scriptPath = resolveScript(args.vendorScript());

            List<String> command = buildCommand(args, scriptPath, videoPath);
            ProcessResult process = run(command, videoPath.getParent());

            ObjectNode details = objectMapper.createObjectNode();
            details.put("tool", "video.loop");
            details.put("reference", "video-loop-finder");
            details.put("videoPath", videoPath.toString());
            details.put("startFrame", args.startFrame());
            if (args.durationHint() != null) {
                details.put("durationHint", args.durationHint());
            }
            details.put("searchRadius", args.searchRadiusOrDefault());
            details.put("vendorScript", scriptPath.toString());

            if (args.outputPath() != null && !args.outputPath().isBlank()) {
                details.put("outputPath", Path.of(args.outputPath()).normalize().toString());
            }

            boolean failed = process.timedOut() || process.exitCode() != 0;
            ProcessExecutionContent structured = new ProcessExecutionContent();
            structured.setStatus(failed ? "failed" : "completed");
            structured.setCommand(command.isEmpty() ? null : command.getFirst());
            structured.setArgs(command.size() <= 1 ? List.of() : command.subList(1, command.size()));
            structured.setWorkingDirectory(videoPath.getParent() == null ? null : videoPath.getParent().toString());
            structured.setExitCode(process.exitCode());
            structured.setTimedOut(process.timedOut());
            structured.setStdout(process.stdout());
            structured.setStderr(process.stderr());
            structured.setDetails(details);

            return DispatchExecutionResult.builder()
                    .appendContent(ResultContent.text(failed ? "Video loop search failed." : "Video loop search completed."))
                    .structuredContent(structured)
                    .error(failed)
                    .status(failed ? "failed" : "ok")
                    .summary(failed ? "video-loop-finder returned a non-zero result" : "loop endpoint search completed")
                    .build();
        } catch (Exception ex) {
            ObjectNode details = objectMapper.createObjectNode()
                    .put("tool", "video.loop")
                    .put("error", ex.getClass().getSimpleName())
                    .put("message", ex.getMessage());
            ProcessExecutionContent structured = new ProcessExecutionContent();
            structured.setStatus("failed");
            structured.setMessage(ex.getMessage());
            structured.setDetails(details);

            return DispatchExecutionResult.builder()
                    .appendContent(ResultContent.text("Video loop search failed: " + ex.getMessage()))
                    .structuredContent(structured)
                    .error("VIDEO_LOOP_FAILED", ex.getMessage())
                    .status("failed")
                    .summary("video.loop failed before or during execution")
                    .build();
        }
    }

    private void validate(VideoLoopFindArgs args) {
        if (args == null) {
            throw new IllegalArgumentException("arguments are required");
        }
        if (args.videoPath() == null || args.videoPath().isBlank()) {
            throw new IllegalArgumentException("videoPath is required");
        }
        if (args.startFrame() < 0) {
            throw new IllegalArgumentException("startFrame must be >= 0");
        }
        if (args.durationHint() != null && args.durationHint() <= 0) {
            throw new IllegalArgumentException("durationHint must be positive when provided");
        }
    }

    private Path resolveScript(String override) {
        Path path = override == null || override.isBlank()
                ? DEFAULT_VENDOR_SCRIPT
                : Path.of(override);
        return path.normalize();
    }

    private List<String> buildCommand(VideoLoopFindArgs args, Path scriptPath, Path videoPath) {
        List<String> command = new ArrayList<>();
        command.add(args.pythonOrDefault());
        command.add(scriptPath.toString());
        command.add("--range");
        command.add(Integer.toString(args.searchRadiusOrDefault()));

        if (args.outputPath() != null && !args.outputPath().isBlank()) {
            command.add("--outfile");
            command.add(Path.of(args.outputPath()).normalize().toString());
        }

        command.add(videoPath.toString());
        command.add(Integer.toString(args.startFrame()));

        if (args.durationHint() != null) {
            command.add(Integer.toString(args.durationHint()));
        }

        return command;
    }

    private ProcessResult run(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null && Files.isDirectory(workingDirectory)) {
            builder.directory(workingDirectory.toFile());
        }

        Process process = builder.start();
        boolean completed = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        if (!completed) {
            process.destroyForcibly();
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = completed ? process.exitValue() : -1;

        return new ProcessResult(exitCode, !completed, stdout, stderr);
    }

    private record ProcessResult(int exitCode, boolean timedOut, String stdout, String stderr) {
    }
}
