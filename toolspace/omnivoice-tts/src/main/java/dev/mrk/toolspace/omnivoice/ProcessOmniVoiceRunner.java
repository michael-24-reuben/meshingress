package dev.mrk.toolspace.omnivoice;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class ProcessOmniVoiceRunner implements OmniVoiceRunner {

    private final ObjectMapper objectMapper;

    ProcessOmniVoiceRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OmniVoiceCommandResult run(List<String> command, JsonNode request, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }

        Process process = builder.start();
        CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
        CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));

        byte[] body = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
        process.getOutputStream().write(body);
        process.getOutputStream().write('\n');
        process.getOutputStream().close();

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
        if (!finished) {
            process.destroyForcibly();
        }

        int exitCode = finished ? process.exitValue() : -1;
        return new OmniVoiceCommandResult(exitCode, get(stdout), get(stderr), !finished, List.copyOf(command));
    }

    private String read(java.io.InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return exception.getMessage() == null ? "" : exception.getMessage();
        }
    }

    private String get(CompletableFuture<String> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            return exception.getMessage() == null ? "" : exception.getMessage();
        }
    }
}
