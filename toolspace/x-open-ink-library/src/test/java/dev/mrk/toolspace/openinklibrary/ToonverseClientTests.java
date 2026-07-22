package dev.mrk.toolspace.openinklibrary;

import com.sun.net.httpserver.HttpServer;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseClient;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseSearchRequest;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseSourceConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToonverseClientTests {
    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void encodesSlugAndSelectsTheConfiguredPayload() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            assertEquals("/api/series/slug/solo%20leveling", exchange.getRequestURI().getRawPath());
            byte[] body = "{\"success\":true,\"data\":{\"id\":\"series-1\"}}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        JsonNode payload = client(ToonverseSourceConfiguration.AuthorizationMode.NONE)
                .fetchSeriesMetadata("solo leveling", null);

        assertEquals("series-1", payload.path("id").asString());
    }

    @Test
    void neverForwardsCredentialsToAConfiguredPublicSource() {
        SourceException exception = assertThrows(
                SourceException.class,
                () -> client(ToonverseSourceConfiguration.AuthorizationMode.NONE)
                        .fetchSeriesMetadata("solo-leveling", "secret-value")
        );

        assertEquals("SOURCE_UNAUTHORIZED", exception.code());
        assertEquals(false, exception.getMessage().contains("secret-value"));
    }

    @Test
    void sendsValidatedChapterLimitAndOrder() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            assertEquals("/api/series/series-1/chapters", exchange.getRequestURI().getRawPath());
            assertEquals("limit=2&offset=0&order=desc", exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"data\":{\"chapters\":[{\"id\":\"chapter-2\"}]}}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        JsonNode payload = client(ToonverseSourceConfiguration.AuthorizationMode.NONE)
                .fetchSeriesChapters("series-1", 2, "desc", null);

        assertEquals("chapter-2", payload.get(0).path("id").asString());
    }

    @Test
    void sendsSupportedSearchFiltersToTheSeriesIndex() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            assertEquals("/api/series", exchange.getRequestURI().getRawPath());
            assertEquals("search=solo+leveling&genres=survival%2Cmonsters&excludeGenres=apocalypse&genreMode=or&type=manhwa&minChapters=51&maxChapters=100&minRating=4.5&maxRating=5.0&author=John+Doe&status=ongoing&sortBy=rating&limit=2&offset=3", exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"data\":{\"items\":[{\"id\":\"series-1\"}],\"total\":1}}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        JsonNode payload = client(ToonverseSourceConfiguration.AuthorizationMode.NONE).searchSeries(new ToonverseSearchRequest(
                "solo leveling", java.util.List.of("survival", "monsters"), java.util.List.of("apocalypse"), "or", "manhwa",
                51, 100, 4.5, 5.0, "John Doe", "ongoing", "rating", 2, 3
        ));

        assertEquals("series-1", payload.path("items").get(0).path("id").asString());
    }

    private ToonverseClient client(ToonverseSourceConfiguration.AuthorizationMode mode) {
        int port = server == null ? 9 : server.getAddress().getPort();
        ToonverseSourceConfiguration configuration = new ToonverseSourceConfiguration(
                "toonverse",
                "Toonverse",
                URI.create("http://127.0.0.1:" + port),
                "/api/series/slug/{slug}",
                "$.data",
                "/api/series/search",
                "$.data",
                "/api/series",
                "$.data",
                "/api/reading/chapter/{slug}/{chapterNumber}",
                "$.data",
                "/api/series/{seriesId}/chapters",
                "$.data.chapters",
                Duration.ofSeconds(2),
                mode,
                20,
                100,
                100,
                500,
                "asc"
        );
        return new ToonverseClient(HttpClient.newHttpClient(), objectMapper, configuration);
    }
}
