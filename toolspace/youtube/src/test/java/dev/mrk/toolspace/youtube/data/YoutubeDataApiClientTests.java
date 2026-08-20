package dev.mrk.toolspace.youtube.data;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeDataApiClientTests {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsTheConfiguredApiKeyAndEscapedPublicSearchParameters() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/youtube/v3/search", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"kind\":\"youtube#searchListResponse\",\"items\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        var response = client("test-key").search(Map.of("part", "snippet", "q", "cats & dogs"));

        assertThat(response.path("kind").asString()).isEqualTo("youtube#searchListResponse");
        assertThat(URLDecoder.decode(query.get(), StandardCharsets.UTF_8))
                .contains("part=snippet", "q=cats & dogs", "key=test-key");
    }

    @Test
    void rejectsBlankCredentialsWithoutCallingTheNetwork() {
        try {
            client("").search(Map.of("part", "snippet", "q", "cats"));
        } catch (YoutubeDataApiException exception) {
            assertThat(exception.code()).isEqualTo("YOUTUBE_API_KEY_NOT_CONFIGURED");
            return;
        }
        throw new AssertionError("Expected a missing-credential failure.");
    }

    private YoutubeDataApiClient client(String apiKey) {
        String baseUrl = server == null
                ? YoutubeDataApiProperties.DEFAULT_BASE_URL
                : "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + "/youtube/v3";
        return new YoutubeDataApiClient(
                new ObjectMapper(),
                new YoutubeDataApiProperties(apiKey, baseUrl, 5_000L),
                HttpClient.newHttpClient()
        );
    }
}
