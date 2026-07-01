package dev.mrk.toolspace.cobalt;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class CobaltClient {

    private static final Set<String> AUDIO_BITRATES = Set.of("320", "256", "128", "96", "64", "8");
    private static final Set<String> AUDIO_FORMATS = Set.of("best", "mp3", "ogg", "wav", "opus");
    private static final Set<String> DOWNLOAD_MODES = Set.of("auto", "audio", "mute");
    private static final Set<String> FILENAME_STYLES = Set.of("classic", "pretty", "basic", "nerdy");
    private static final Set<String> VIDEO_QUALITIES = Set.of("max", "4320", "2160", "1440", "1080", "720", "480", "360", "240", "144");
    private static final Set<String> LOCAL_PROCESSING_MODES = Set.of("disabled", "preferred", "forced");
    private static final Set<String> YOUTUBE_VIDEO_CODECS = Set.of("h264", "av1", "vp9");
    private static final Set<String> YOUTUBE_VIDEO_CONTAINERS = Set.of("auto", "mp4", "webm", "mkv");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final Optional<String> authHeader;
    private final String userAgent;

    CobaltClient(HttpClient httpClient, ObjectMapper objectMapper, String baseUrl, String authHeader, String userAgent) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.baseUri = normalizeBaseUri(baseUrl);
        this.authHeader = normalizeOptional(authHeader);
        this.userAgent = normalizeOptional(userAgent).orElse("Meshingress-Cobalt/0.1");
    }

    JsonNode info() throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(resolve(""))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET();
        addAuth(request);
        return sendJson(request.build());
    }

    JsonNode process(CobaltProcessArgs args) throws IOException, InterruptedException {
        if (args == null || args.url() == null || args.url().isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        validateSourceUrl(args.url());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("url", args.url().trim());
        putOption(body, "audioBitrate", args.audioBitrate(), AUDIO_BITRATES);
        putOption(body, "audioFormat", args.audioFormat(), AUDIO_FORMATS);
        putOption(body, "downloadMode", args.downloadMode(), DOWNLOAD_MODES);
        putOption(body, "filenameStyle", args.filenameStyle(), FILENAME_STYLES);
        putOption(body, "videoQuality", args.videoQuality(), VIDEO_QUALITIES);
        putOption(body, "localProcessing", args.localProcessing(), LOCAL_PROCESSING_MODES);
        putString(body, "subtitleLang", args.subtitleLang());
        putOption(body, "youtubeVideoCodec", args.youtubeVideoCodec(), YOUTUBE_VIDEO_CODECS);
        putOption(body, "youtubeVideoContainer", args.youtubeVideoContainer(), YOUTUBE_VIDEO_CONTAINERS);
        putString(body, "youtubeDubLang", args.youtubeDubLang());
        putBoolean(body, "disableMetadata", args.disableMetadata());
        putBoolean(body, "alwaysProxy", args.alwaysProxy());
        putBoolean(body, "convertGif", args.convertGif());
        putBoolean(body, "allowH265", args.allowH265());
        putBoolean(body, "tiktokFullAudio", args.tiktokFullAudio());
        putBoolean(body, "youtubeBetterAudio", args.youtubeBetterAudio());
        putBoolean(body, "youtubeHLS", args.youtubeHLS());

        HttpRequest.Builder request = HttpRequest.newBuilder(resolve(""))
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
        addAuth(request);
        return sendJson(request.build());
    }

    URI baseUri() {
        return baseUri;
    }

    private JsonNode sendJson(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        String body = Optional.ofNullable(response.body()).orElse("");
        JsonNode parsed = parseBody(body);
        if (status < 200 || status >= 300) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("statusCode", status);
            error.set("body", parsed);
            throw new CobaltHttpException("Cobalt HTTP " + status, error);
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

    private void addAuth(HttpRequest.Builder request) {
        authHeader.ifPresent(value -> request.header("Authorization", value));
    }

    private static URI normalizeBaseUri(String value) {
        String raw = value == null || value.isBlank() ? "http://127.0.0.1:9000" : value.trim();
        if (!raw.endsWith("/")) {
            raw = raw + "/";
        }
        URI uri = URI.create(raw);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Cobalt base URL must use http or https");
        }
        return uri;
    }

    private static Optional<String> normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private static void validateSourceUrl(String value) {
        URI uri = URI.create(value.trim());
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("url must use http or https");
        }
    }

    private static void putString(ObjectNode body, String field, String value) {
        if (value != null && !value.isBlank()) {
            body.put(field, value.trim());
        }
    }

    private static void putBoolean(ObjectNode body, String field, Boolean value) {
        if (value != null) {
            body.put(field, value);
        }
    }

    private static void putOption(ObjectNode body, String field, String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim();
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException(field + " must be one of: " + String.join(", ", allowed));
        }
        body.put(field, normalized);
    }

    static String userMessage(Exception exception) {
        if (exception instanceof ConnectException || exception.getCause() instanceof ConnectException) {
            return "Could not connect to Cobalt. Start a local Cobalt API instance from toolspace/cobalt/upstream/cobalt or set meshingress.cobalt.base-url.";
        }
        return exception.getMessage() == null ? "Cobalt request failed." : exception.getMessage();
    }
}
