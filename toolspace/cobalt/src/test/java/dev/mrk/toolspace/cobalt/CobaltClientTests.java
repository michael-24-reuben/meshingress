package dev.mrk.toolspace.cobalt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobaltClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void processPostsDocumentedCobaltPayload() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        startServer(exchange -> {
            method.set(exchange.getRequestMethod());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"status":"redirect","url":"https://cdn.example/media.mp4","filename":"media.mp4"}
                    """);
        });

        CobaltClient client = client("Api-Key test-key");
        var response = client.process(new CobaltProcessArgs(
                "https://example.com/watch?v=1",
                "320",
                "mp3",
                "audio",
                "basic",
                "1080",
                true,
                null,
                "disabled",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null
        ));

        assertEquals("POST", method.get());
        assertEquals("Api-Key test-key", auth.get());
        assertEquals("redirect", response.get("status").asText());

        var request = objectMapper.readTree(body.get());
        assertEquals("https://example.com/watch?v=1", request.get("url").asText());
        assertEquals("320", request.get("audioBitrate").asText());
        assertEquals("mp3", request.get("audioFormat").asText());
        assertEquals("audio", request.get("downloadMode").asText());
        assertEquals("basic", request.get("filenameStyle").asText());
        assertEquals("1080", request.get("videoQuality").asText());
        assertEquals(true, request.get("disableMetadata").booleanValue());
        assertEquals("disabled", request.get("localProcessing").asText());
        assertEquals(true, request.get("youtubeBetterAudio").booleanValue());
    }

    @Test
    void infoReadsInstanceMetadata() throws Exception {
        startServer(exchange -> respond(exchange, 200, """
                {"cobalt":{"version":"11.7.1","services":["youtube"]},"git":{"commit":"abc"}}
                """));

        CobaltClient client = client("");
        var response = client.info();

        assertEquals("11.7.1", response.get("cobalt").get("version").asText());
        assertEquals("youtube", response.get("cobalt").get("services").get(0).asText());
    }

    @Test
    void rejectsUnsupportedOptionBeforeNetworkCall() {
        CobaltClient client = new CobaltClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(100)).build(),
                objectMapper,
                "http://127.0.0.1:1",
                "",
                "test"
        );

        assertThrows(IllegalArgumentException.class, () -> client.process(new CobaltProcessArgs(
                "https://example.com/watch?v=1",
                "999",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )));
    }

    private CobaltClient client(String authHeader) {
        return new CobaltClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                objectMapper,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                authHeader,
                "test"
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
