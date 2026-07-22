package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.toolspace.openinklibrary.SourceException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ToonverseClient {
    public record DownloadedMedia(InputStream content, String mimeType) implements AutoCloseable {
        @Override public void close() throws IOException { content.close(); }
    }
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ToonverseSourceConfiguration configuration;

    public ToonverseClient(HttpClient httpClient, ObjectMapper objectMapper, ToonverseSourceConfiguration configuration) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
    }

    public JsonNode fetchSeriesMetadata(String slug, String authorizationToken) {
        if (slug == null || slug.isBlank()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "slug must be a non-blank string.");
        }
        URI uri = metadataUri(slug.trim());
        return getJson(uri, configuration.metadataSelector(), authorizationToken, "metadata");
    }

    public JsonNode searchSeriesByName(String name) {
        String normalizedName = requiredName(name);
        return getJson(nameSearchUri(normalizedName), configuration.nameSearchSelector(), null, "name-search");
    }

    public JsonNode searchSeries(ToonverseSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return getJson(searchUri(request), configuration.searchSelector(), null, "search");
    }

    public JsonNode fetchSeriesChapters(String seriesId, Integer requestedLimit, String requestedOrder, String authorizationToken) {
        JsonNode page = fetchSeriesChapterPage(seriesId, requestedLimit, requestedOrder, authorizationToken);
        JsonNode chapters = page.path("chapters");
        if (!chapters.isArray()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The source chapter page did not contain a chapters array.");
        }
        return chapters;
    }

    public JsonNode fetchSeriesChapterPage(String seriesId, Integer requestedLimit, String requestedOrder, String authorizationToken) {
        return fetchSeriesChapterPage(seriesId, requestedLimit, null, requestedOrder, authorizationToken);
    }

    public JsonNode fetchSeriesChapterPage(String seriesId, Integer requestedLimit, Integer requestedOffset, String requestedOrder, String authorizationToken) {
        if (seriesId == null || seriesId.isBlank()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "sourceBookId must be a non-blank string.");
        }
        int limit = chapterLimit(requestedLimit);
        int offset = nonNegative("offset", requestedOffset, 0);
        String order = chapterOrder(requestedOrder);
        URI uri = chaptersUri(seriesId.trim(), limit, offset, order);
        return getJson(uri, "$.data", authorizationToken, "chapter page");
    }

    public JsonNode fetchReadingChapter(String slug, Integer chapterNumber) {
        if (slug == null || slug.isBlank()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "slug must be a non-blank string.");
        }
        if (chapterNumber == null || chapterNumber < 0) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "chapterNumber must be a non-negative integer.");
        }
        return getJson(readingChapterUri(slug.trim(), chapterNumber), configuration.readingChapterSelector(), null, "reading chapter");
    }

    public DownloadedMedia downloadMedia(String url) {
        if (url == null || url.isBlank()) throw new SourceException("SOURCE_RESPONSE_INVALID", "The source page is missing imageUrl.");
        URI uri;
        try { uri = URI.create(url.trim()); } catch (IllegalArgumentException exception) { throw new SourceException("SOURCE_RESPONSE_INVALID", "The source returned an invalid media URL.", exception); }
        String host = uri.getHost();
        String sourceHost = configuration.apiBaseUrl().getHost();
        if ((!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || host == null)
                || !(host.equalsIgnoreCase(sourceHost) || host.endsWith(".toonverse.net") || host.equalsIgnoreCase("toonverse.net"))) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The source returned a media URL outside Toonverse.");
        }
        HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(configuration.timeout()).header("Accept", "image/*").build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw failureForStatus(response.statusCode());
            }
            String mime = response.headers().firstValue("Content-Type").orElse("application/octet-stream").split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            return new DownloadedMedia(response.body(), mime);
        } catch (SourceException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SourceException("SOURCE_REQUEST_FAILED", "The media request was interrupted.", exception);
        } catch (IOException exception) {
            throw new SourceException("SOURCE_REQUEST_FAILED", "The media request failed.", exception);
        }
    }

    private JsonNode getJson(URI uri, String selector, String authorizationToken, String payloadName) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(configuration.timeout())
                .header("Accept", "application/json");
        applyAuthorization(request, authorizationToken);

        try {
            HttpResponse<String> response = sendWithRetry(request.build());
            if (response.statusCode() != 200) {
                throw failureForStatus(response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return select(root, selector, payloadName);
        } catch (SourceException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SourceException("SOURCE_REQUEST_FAILED", "The source request was interrupted.", exception);
        } catch (IOException exception) {
            throw new SourceException("SOURCE_REQUEST_FAILED", "The source request failed.", exception);
        }
    }
    
    private URI metadataUri(String slug) {
        String encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8).replace("+", "%20");
        String path = configuration.metadataPath().replace("{slug}", encodedSlug);
        return configuration.apiBaseUrl().resolve(path);
    }

    private URI chaptersUri(String seriesId, int limit, int offset, String order) {
        String encodedId = URLEncoder.encode(seriesId, StandardCharsets.UTF_8).replace("+", "%20");
        String path = configuration.chaptersPath().replace("{seriesId}", encodedId);
        return configuration.apiBaseUrl().resolve(path + "?limit=" + limit + "&offset=" + offset + "&order=" + order);
    }

    private URI readingChapterUri(String slug, int chapterNumber) {
        String encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8).replace("+", "%20");
        String path = configuration.readingChapterPath()
                .replace("{slug}", encodedSlug)
                .replace("{chapterNumber}", Integer.toString(chapterNumber));
        return configuration.apiBaseUrl().resolve(path);
    }

    private URI nameSearchUri(String name) {
        return configuration.apiBaseUrl().resolve(configuration.nameSearchPath() + "?q=" + queryValue(name));
    }

    private URI searchUri(ToonverseSearchRequest request) {
        List<String> parameters = new ArrayList<>();
        add(parameters, "search", optionalText(request.name()));
        addJoined(parameters, "genres", request.genres());
        addJoined(parameters, "excludeGenres", request.excludeGenres());
        add(parameters, "genreMode", allowed("genreMode", request.genreMode(), Set.of("or", "and")));
        add(parameters, "type", allowed("type", request.type(), Set.of("manhwa", "manhua", "manga")));
        addRange(parameters, "minChapters", request.minChapters(), "maxChapters", request.maxChapters(), 0, Integer.MAX_VALUE);
        addRange(parameters, "minRating", request.minRating(), "maxRating", request.maxRating(), 0.0, 5.0);
        add(parameters, "author", optionalText(request.author()));
        add(parameters, "status", allowed("status", request.status(), Set.of("ongoing", "completed", "hiatus")));
        add(parameters, "sortBy", sortBy(request.sortBy()));
        add(parameters, "limit", Integer.toString(searchLimit(request.limit())));
        add(parameters, "offset", Integer.toString(nonNegative("offset", request.offset(), 0)));
        return configuration.apiBaseUrl().resolve(configuration.searchPath() + "?" + String.join("&", parameters));
    }

    private int chapterLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return configuration.defaultChapterLimit();
        }
        if (requestedLimit <= 0 || requestedLimit > configuration.maximumChapterLimit()) {
            throw new SourceException(
                    "SOURCE_RESPONSE_INVALID",
                    "limit must be between 1 and " + configuration.maximumChapterLimit() + "."
            );
        }
        return requestedLimit;
    }

    private int searchLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return configuration.defaultSearchLimit();
        }
        if (requestedLimit <= 0 || requestedLimit > configuration.maximumSearchLimit()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "limit must be between 1 and " + configuration.maximumSearchLimit() + ".");
        }
        return requestedLimit;
    }

    private String chapterOrder(String requestedOrder) {
        if (requestedOrder == null || requestedOrder.isBlank()) {
            return configuration.defaultChapterOrder();
        }
        String order = requestedOrder.trim().toLowerCase(java.util.Locale.ROOT);
        if (!order.equals("asc") && !order.equals("desc")) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "order must be asc or desc.");
        }
        return order;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 429 && response.statusCode() < 500) {
            return response;
        }
        Thread.sleep(200);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void applyAuthorization(HttpRequest.Builder request, String token) {
        boolean supplied = token != null && !token.isBlank();
        if (configuration.authorizationMode() == ToonverseSourceConfiguration.AuthorizationMode.NONE) {
            if (supplied) {
                throw new SourceException("SOURCE_UNAUTHORIZED", "This source does not accept caller authorization.");
            }
            return;
        }
        if (configuration.authorizationMode() == ToonverseSourceConfiguration.AuthorizationMode.BEARER && !supplied) {
            throw new SourceException("SOURCE_UNAUTHORIZED", "This source requires caller authorization.");
        }
        if (supplied) {
            request.header("Authorization", "Bearer " + token.trim());
        }
    }

    private static SourceException failureForStatus(int status) {
        return switch (status) {
            case 401, 403 -> new SourceException("SOURCE_UNAUTHORIZED", "The source rejected the request authorization.");
            case 404 -> new SourceException("SOURCE_WORK_NOT_FOUND", "The requested work was not found by the source.");
            case 429 -> new SourceException("SOURCE_RATE_LIMITED", "The source rate-limited this request.");
            default -> new SourceException("SOURCE_REQUEST_FAILED", "The source returned HTTP " + status + ".");
        };
    }

    private static String requiredName(String value) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "name must be a non-blank string.");
        }
        return normalized;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String allowed(String field, String value, Set<String> allowed) {
        String normalized = optionalText(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", field + " is not supported by Toonverse.");
        }
        return normalized;
    }

    private static String sortBy(String value) {
        String normalized = optionalText(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals("liibrary")) {
            normalized = "library";
        }
        if (!Set.of("popular", "trending", "updated", "rating", "library", "newest", "chapters", "alphabetical").contains(normalized)) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "sortBy is not supported by Toonverse.");
        }
        return normalized;
    }

    private static int nonNegative(String field, Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value < 0) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", field + " must not be negative.");
        }
        return value;
    }

    private static void addRange(List<String> parameters, String minField, Integer min, String maxField, Integer max, int lowerBound, int upperBound) {
        if (min != null && (min < lowerBound || min > upperBound) || max != null && (max < lowerBound || max > upperBound) || min != null && max != null && min > max) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", minField + " and " + maxField + " must be an ordered range within supported bounds.");
        }
        add(parameters, minField, min == null ? null : min.toString());
        add(parameters, maxField, max == null ? null : max.toString());
    }

    private static void addRange(List<String> parameters, String minField, Double min, String maxField, Double max, double lowerBound, double upperBound) {
        if (min != null && (!Double.isFinite(min) || min < lowerBound || min > upperBound) || max != null && (!Double.isFinite(max) || max < lowerBound || max > upperBound) || min != null && max != null && min > max) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", minField + " and " + maxField + " must be an ordered range within supported bounds.");
        }
        add(parameters, minField, min == null ? null : min.toString());
        add(parameters, maxField, max == null ? null : max.toString());
    }

    private static void addJoined(List<String> parameters, String key, List<String> values) {
        List<String> normalized = values.stream().map(ToonverseClient::optionalText).filter(Objects::nonNull).toList();
        if (normalized.size() != values.size()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", key + " must not include blank values.");
        }
        add(parameters, key, normalized.isEmpty() ? null : String.join(",", normalized));
    }

    private static void add(List<String> parameters, String key, String value) {
        if (value != null) {
            parameters.add(key + "=" + queryValue(value));
        }
    }

    private static String queryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static JsonNode select(JsonNode root, String selector, String payloadName) {
        JsonNode selected = root;
        for (String segment : selector.substring(2).split("\\.")) {
            selected = selected.path(segment);
        }
        if (selected.isMissingNode() || selected.isNull()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The source response did not contain the configured " + payloadName + " payload.");
        }
        return selected;
    }
}
