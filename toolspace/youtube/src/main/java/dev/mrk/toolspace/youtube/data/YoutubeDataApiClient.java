package dev.mrk.toolspace.youtube.data;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal HTTP adapter for public YouTube Data API v3 reads using the configured API key. */
public final class YoutubeDataApiClient {
    private final ObjectMapper objectMapper;
    private final YoutubeDataApiProperties properties;
    private final HttpClient httpClient;

    public YoutubeDataApiClient(ObjectMapper objectMapper, YoutubeDataApiProperties properties, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public JsonNode search(Map<String, String> parameters) {
        return get("search", parameters);
    }

    public JsonNode videos(Map<String, String> parameters) {
        return get("videos", parameters);
    }

    public JsonNode channels(Map<String, String> parameters) {
        return get("channels", parameters);
    }

    public JsonNode playlists(Map<String, String> parameters) {
        return get("playlists", parameters);
    }

    private JsonNode get(String resource, Map<String, String> parameters) {
        if (properties.apiKey().isBlank()) {
            throw new YoutubeDataApiException(
                    "YOUTUBE_API_KEY_NOT_CONFIGURED",
                    "meshingress.youtube.api-key is blank. Set it in the ignored production property file before calling public YouTube Data API functions."
            );
        }

        Map<String, String> query = new LinkedHashMap<>();
        if (parameters != null) {
            parameters.forEach((name, value) -> {
                if (name != null && value != null && !value.isBlank()) {
                    query.put(name, value);
                }
            });
        }
        query.put("key", properties.apiKey());

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri(resource, query))
                    .GET()
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(properties.requestTimeoutMs()))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new YoutubeDataApiException("YOUTUBE_DATA_API_CONFIGURATION_INVALID", "YouTube Data API base URL is invalid.");
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw upstreamFailure(response.statusCode(), response.body());
            }
            JsonNode body = objectMapper.readTree(response.body());
            if (body == null || !body.isObject()) {
                throw new YoutubeDataApiException("YOUTUBE_DATA_API_RESPONSE_INVALID", "YouTube Data API returned an invalid JSON response.");
            }
            return body;
        } catch (YoutubeDataApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new YoutubeDataApiException("YOUTUBE_DATA_API_INTERRUPTED", "YouTube Data API request was interrupted.");
        } catch (Exception exception) {
            throw new YoutubeDataApiException("YOUTUBE_DATA_API_UNAVAILABLE", "Unable to call the YouTube Data API.");
        }
    }

    private URI uri(String resource, Map<String, String> query) {
        String queryString = query.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return URI.create(properties.dataApiBaseUrl() + "/" + resource + "?" + queryString);
    }

    private YoutubeDataApiException upstreamFailure(int status, String body) {
        String reason = "";
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            reason = error.path("errors").path(0).path("reason").asString("");
        } catch (Exception ignored) {
            // Deliberately avoid exposing the raw upstream body, which may contain request details.
        }
        String detail = reason.isBlank() ? "" : " (" + reason + ")";
        return new YoutubeDataApiException("YOUTUBE_DATA_API_HTTP_" + status, "YouTube Data API returned HTTP " + status + detail + ".");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
