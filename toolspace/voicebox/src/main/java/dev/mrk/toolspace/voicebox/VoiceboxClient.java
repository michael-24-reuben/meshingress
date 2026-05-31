package dev.mrk.toolspace.voicebox;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class VoiceboxClient {

    private static final long MAX_TRANSCRIBE_BYTES = 200L * 1024L * 1024L;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String clientId;

    VoiceboxClient(HttpClient httpClient, ObjectMapper objectMapper, String baseUrl, String clientId) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.baseUri = normalizeBaseUri(baseUrl);
        this.clientId = clientId == null || clientId.isBlank() ? "meshingress" : clientId.trim();
    }

    JsonNode health() throws IOException, InterruptedException {
        return sendJson(get("/health", Duration.ofSeconds(10)));
    }

    JsonNode listProfiles() throws IOException, InterruptedException {
        return sendJson(get("/profiles", Duration.ofSeconds(15)));
    }

    JsonNode speak(VoiceboxSpeakArgs args) throws IOException, InterruptedException {
        if (args == null || args.text() == null || args.text().isBlank()) {
            throw new IllegalArgumentException("text is required");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("text", args.text());
        putIfPresent(body, "profile", args.profile());
        putIfPresent(body, "engine", args.engine());
        putIfPresent(body, "language", args.language());
        if (args.personality() != null) {
            body.put("personality", args.personality());
        }

        return sendJson(postJson("/speak", body, Duration.ofMinutes(2)));
    }

    JsonNode generationStatus(VoiceboxGenerationStatusArgs args) throws IOException, InterruptedException {
        if (args == null || args.generationId() == null || args.generationId().isBlank()) {
            throw new IllegalArgumentException("generationId is required");
        }
        return sendText(get("/generate/" + pathSegment(args.generationId()) + "/status", Duration.ofSeconds(30)));
    }

    JsonNode transcribeFile(VoiceboxTranscribeFileArgs args) throws IOException, InterruptedException {
        if (args == null || args.audioPath() == null || args.audioPath().isBlank()) {
            throw new IllegalArgumentException("audioPath is required");
        }

        Path path = Path.of(args.audioPath()).toAbsolutePath().normalize();
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("audioPath must resolve to an absolute path");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("audioPath is not a readable file: " + path);
        }
        long size = Files.size(path);
        if (size > MAX_TRANSCRIBE_BYTES) {
            throw new IllegalArgumentException("audioPath exceeds the 200 MB Voicebox transcription limit");
        }

        String boundary = "meshingress-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, path, args.language(), args.model());
        HttpRequest request = HttpRequest.newBuilder(resolve("/transcribe"))
                .timeout(Duration.ofMinutes(10))
                .header("X-Voicebox-Client-Id", clientId)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return sendJson(request);
    }

    private HttpRequest get(String path, Duration timeout) {
        return HttpRequest.newBuilder(resolve(path))
                .timeout(timeout)
                .header("X-Voicebox-Client-Id", clientId)
                .GET()
                .build();
    }

    private HttpRequest postJson(String path, ObjectNode body, Duration timeout) throws IOException {
        return HttpRequest.newBuilder(resolve(path))
                .timeout(timeout)
                .header("X-Voicebox-Client-Id", clientId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    private JsonNode sendJson(HttpRequest request) throws IOException, InterruptedException {
        JsonNode response = sendText(request);
        if (!response.isTextual()) {
            return response;
        }
        return objectMapper.readTree(response.asText());
    }

    private JsonNode sendText(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        String body = Optional.ofNullable(response.body()).orElse("");
        JsonNode parsed = parseBody(body);
        if (status < 200 || status >= 300) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("statusCode", status);
            error.set("body", parsed);
            throw new VoiceboxHttpException("Voicebox HTTP " + status, error);
        }
        return parsed;
    }

    private JsonNode parseBody(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return objectMapper.readTree(trimmed);
        }
        return objectMapper.valueToTree(body);
    }

    private URI resolve(String path) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return baseUri.resolve(normalizedPath);
    }

    private static URI normalizeBaseUri(String value) {
        String raw = value == null || value.isBlank() ? "http://127.0.0.1:17493" : value.trim();
        if (!raw.endsWith("/")) {
            raw = raw + "/";
        }
        return URI.create(raw);
    }

    private static String pathSegment(String value) {
        return value.replace("/", "%2F").replace("\\", "%5C");
    }

    private void putIfPresent(ObjectNode body, String field, String value) {
        if (value != null && !value.isBlank()) {
            body.put(field, value.trim());
        }
    }

    private byte[] multipartBody(String boundary, Path file, String language, String model) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeFormField(out, boundary, "language", language);
        writeFormField(out, boundary, "model", model);
        writeAscii(out, "--" + boundary + "\r\n");
        writeAscii(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename(file) + "\"\r\n");
        writeAscii(out, "Content-Type: application/octet-stream\r\n\r\n");
        Files.copy(file, out);
        writeAscii(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private void writeFormField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        writeAscii(out, "--" + boundary + "\r\n");
        writeAscii(out, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.write(value.trim().getBytes(StandardCharsets.UTF_8));
        writeAscii(out, "\r\n");
    }

    private void writeAscii(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static String safeFilename(Path file) {
        String name = Optional.ofNullable(file.getFileName()).map(Path::toString).orElse("audio");
        return name.replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }
}
