package dev.mrk.toolspace.transform;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

final class TransformBackend {
    private final ObjectMapper objectMapper;
    private final TransformProperties properties;

    TransformBackend(ObjectMapper objectMapper, TransformProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    String transform(TransformArgs arguments) throws Exception {
        Path backend = Path.of(properties.backendDirectory()).toAbsolutePath().normalize();
        Path bridge = backend.resolve("transform-bridge.cjs");
        if (!Files.isRegularFile(bridge)) {
            throw new IllegalStateException("Transform backend bridge is unavailable: " + bridge);
        }
        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", arguments.type().id());
        request.put("value", arguments.value());
        if (arguments.secondaryValue() != null) request.put("secondaryValue", arguments.secondaryValue());
        if (arguments.settings() != null && !arguments.settings().isNull()) request.set("settings", arguments.settings());

        Process process = new ProcessBuilder(List.of(properties.nodeExecutable(), bridge.toString()))
                .directory(backend.toFile())
                .start();
        process.getOutputStream().write(objectMapper.writeValueAsBytes(request));
        process.getOutputStream().close();
        CompletableFuture<String> stdout = read(process.getInputStream());
        CompletableFuture<String> stderr = read(process.getErrorStream());
        if (!process.waitFor(properties.timeoutMs(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Transform backend timed out after " + properties.timeoutMs() + " ms");
        }
        String response = stdout.get(10, TimeUnit.SECONDS);
        String error = stderr.get(10, TimeUnit.SECONDS);
        if (process.exitValue() != 0) throw new IllegalStateException("Transform backend failed: " + truncate(error));
        JsonNode payload = objectMapper.readTree(response);
        if (!payload.path("ok").asBoolean()) throw new IllegalArgumentException(payload.path("error").asString("Transform backend rejected the request."));
        return payload.path("result").asString();
    }

    private static CompletableFuture<String> read(java.io.InputStream input) {
        return CompletableFuture.supplyAsync(() -> {
            try (input) { return new String(input.readAllBytes(), StandardCharsets.UTF_8); }
            catch (IOException exception) { return ""; }
        });
    }
    private static String truncate(String value) { return value.length() <= 4_000 ? value : value.substring(0, 4_000); }
}
