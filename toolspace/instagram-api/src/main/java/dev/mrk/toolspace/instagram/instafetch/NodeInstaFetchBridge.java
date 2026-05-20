package dev.mrk.toolspace.instagram.instafetch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class NodeInstaFetchBridge implements InstaFetchBridge {
    private static final String PACKAGE_RESOURCE = "/dev/mrk/toolspace/instagram/instafetch-1.0.0.tgz";
    private static final String RUNNER_RESOURCE = "/dev/mrk/toolspace/instagram/instafetch-runner.mjs";

    private final ObjectMapper objectMapper;
    private final Path runtimeDirectory;
    private final String nodeExecutable;
    private final String npmExecutable;
    private final Duration timeout;

    private boolean initialized;

    public NodeInstaFetchBridge(ObjectMapper objectMapper, Path runtimeDirectory) {
        this(objectMapper, runtimeDirectory, defaultNodeExecutable(), defaultNpmExecutable(), Duration.ofSeconds(60));
    }

    public NodeInstaFetchBridge(
            ObjectMapper objectMapper,
            Path runtimeDirectory,
            String nodeExecutable,
            String npmExecutable,
            Duration timeout
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.runtimeDirectory = Objects.requireNonNull(runtimeDirectory, "runtimeDirectory");
        this.nodeExecutable = Objects.requireNonNull(nodeExecutable, "nodeExecutable");
        this.npmExecutable = Objects.requireNonNull(npmExecutable, "npmExecutable");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    public static NodeInstaFetchBridge createDefault(ObjectMapper objectMapper) {
        return new NodeInstaFetchBridge(objectMapper, Path.of(System.getProperty("java.io.tmpdir"), "meshingress-instafetch"));
    }

    @Override
    public JsonNode submitRequest(FetchPath path, InstaFetchOptions options) {
        JsonNode result = execute("submitRequest", List.of(path), options);
        if (result.isMissingNode() || result.isNull()) {
            throw new InstaFetchBridgeException("Node bridge returned no result for submitRequest");
        }
        return result;
    }

    @Override
    public List<JsonNode> submitAllRequests(List<FetchPath> paths, InstaFetchOptions options) {
        JsonNode result = execute("submitAllRequests", paths, options);
        if (!result.isArray()) {
            throw new InstaFetchBridgeException("Node bridge returned a non-array result for submitAllRequests");
        }
        List<JsonNode> values = new ArrayList<>();
        result.forEach(values::add);
        return List.copyOf(values);
    }

    @Override
    public List<InstaFetchSettledResult> settleAllRequests(List<FetchPath> paths, InstaFetchOptions options) {
        JsonNode result = execute("settleAllRequests", paths, options);
        if (!result.isArray()) {
            throw new InstaFetchBridgeException("Node bridge returned a non-array result for settleAllRequests");
        }
        List<InstaFetchSettledResult> values = new ArrayList<>();
        for (JsonNode item : result) {
            FetchPath path = pathFromNode(item.path("path"));
            if (item.path("ok").asBoolean(false)) {
                values.add(InstaFetchSettledResult.success(path, item.path("data")));
            } else {
                values.add(InstaFetchSettledResult.failure(path, item.path("error")));
            }
        }
        return List.copyOf(values);
    }

    private JsonNode execute(String operation, List<FetchPath> paths, InstaFetchOptions options) {
        ensureRuntime();
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operation", operation);
        request.set("media", mediaArray(paths));
        request.set("options", optionsNode(options == null ? InstaFetchOptions.defaults() : options));

        ProcessBuilder processBuilder = new ProcessBuilder(nodeExecutable, "instafetch-runner.mjs");
        processBuilder.directory(runtimeDirectory.toFile());

        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdout = readStream(process.getInputStream());
            CompletableFuture<String> stderr = readStream(process.getErrorStream());
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(objectMapper.writeValueAsString(request));
            }

            boolean exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                throw new InstaFetchBridgeException("Node bridge timed out after " + timeout.toSeconds() + " seconds");
            }
            String stdoutText = joinStream(stdout);
            String stderrText = joinStream(stderr);
            if (process.exitValue() != 0) {
                throw new InstaFetchBridgeException("Node bridge failed with exit code %d: %s"
                        .formatted(process.exitValue(), stderrText.trim()));
            }

            JsonNode envelope = objectMapper.readTree(stdoutText);
            if (!envelope.path("ok").asBoolean(false)) {
                throw new InstaFetchBridgeException(envelope.path("error").path("message").asString("Node bridge request failed"));
            }
            return envelope.path("result");
        } catch (IOException exception) {
            throw new InstaFetchBridgeException("Unable to execute Node bridge", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InstaFetchBridgeException("Interrupted while executing Node bridge", exception);
        }
    }

    private synchronized void ensureRuntime() {
        if (initialized && Files.exists(runtimeDirectory.resolve("node_modules").resolve("instafetch").resolve("package.json"))) {
            return;
        }
        try {
            Files.createDirectories(runtimeDirectory);
            copyResource(PACKAGE_RESOURCE, runtimeDirectory.resolve("instafetch-1.0.0.tgz"));
            copyResource(RUNNER_RESOURCE, runtimeDirectory.resolve("instafetch-runner.mjs"));
            installPackage();
            initialized = true;
        } catch (IOException exception) {
            throw new InstaFetchBridgeException("Unable to prepare bundled instafetch runtime", exception);
        }
    }

    private void installPackage() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                npmExecutable,
                "install",
                runtimeDirectory.resolve("instafetch-1.0.0.tgz").toString(),
                "--omit=dev",
                "--no-audit",
                "--no-fund"
        );
        processBuilder.directory(runtimeDirectory.toFile());
        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdout = readStream(process.getInputStream());
            CompletableFuture<String> stderr = readStream(process.getErrorStream());
            boolean exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                throw new InstaFetchBridgeException("npm install for instafetch timed out after " + timeout.toSeconds() + " seconds");
            }
            joinStream(stdout);
            String stderrText = joinStream(stderr);
            if (process.exitValue() != 0) {
                throw new InstaFetchBridgeException("npm install for instafetch failed with exit code %d: %s"
                        .formatted(process.exitValue(), stderrText.trim()));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InstaFetchBridgeException("Interrupted while installing bundled instafetch runtime", exception);
        }
    }

    private void copyResource(String resource, Path target) throws IOException {
        try (InputStream inputStream = NodeInstaFetchBridge.class.getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new InstaFetchBridgeException("Required resource is missing: " + resource);
            }
            Files.copy(inputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ArrayNode mediaArray(List<FetchPath> paths) {
        ArrayNode media = objectMapper.createArrayNode();
        paths.forEach(path -> {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("kind", path.kind().jsonValue());
            item.put("value", path.value());
            item.put("shortcode", path.shortcode());
            media.add(item);
        });
        return media;
    }

    private ObjectNode optionsNode(InstaFetchOptions options) {
        ObjectNode node = objectMapper.createObjectNode();
        if (!options.headers().isEmpty()) {
            ObjectNode headers = objectMapper.createObjectNode();
            options.headers().forEach(headers::put);
            node.set("headers", headers);
        }
        if (options.userAgent() != null) {
            node.put("userAgent", options.userAgent());
        }
        if (options.cookie() != null) {
            node.put("cookie", options.cookie());
        }
        if (!options.requestConfig().isEmpty()) {
            node.set("requestConfig", objectMapper.valueToTree(options.requestConfig().values()));
        }
        return node;
    }

    private FetchPath pathFromNode(JsonNode node) {
        String value = node.path("value").asString(null);
        if (value != null) {
            return FetchPath.from(value);
        }
        return FetchPath.asShortcode(node.path("shortcode").asString());
    }

    private CompletableFuture<String> readStream(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        });
    }

    private String joinStream(CompletableFuture<String> stream) {
        try {
            return stream.join();
        } catch (CompletionException exception) {
            throw new InstaFetchBridgeException("Unable to read child process output", exception);
        }
    }

    private static String defaultNodeExecutable() {
        return "node";
    }

    private static String defaultNpmExecutable() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "npm.cmd" : "npm";
    }
}
