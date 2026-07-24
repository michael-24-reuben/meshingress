package dev.mrk.toolspace.openinklibrary;

import com.sun.net.httpserver.HttpServer;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseClient;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseFetchArgs;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseFetchChapterArgs;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseFetchChaptersArgs;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseFetchFullArgs;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseDownloadBookArgs;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseSourceConfiguration;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseTool;
import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.result.progress.ProgressUpdate;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageTransferMode;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.io.InputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToonverseToolTests {
    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchReturnsMetadataWithoutRequestingChapters() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            if (exchange.getRequestURI().getPath().equals("/api/series/search")) {
                assertEquals("q=Solo+Leveling", exchange.getRequestURI().getRawQuery());
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else if (exchange.getRequestURI().getPath().equals("/api/series/slug/solo-leveling")) {
                assertEquals("/api/series/slug/solo-leveling", exchange.getRequestURI().getPath());
                body = "{\"data\":{\"id\":\"series-1\",\"title\":\"Solo Leveling\"}}".getBytes();
            } else {
                throw new AssertionError("toonverse.fetch must not request chapters");
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper);

        var result = tool.fetch(new ToonverseFetchArgs("Solo Leveling"), null);

        assertEquals(false, result.isError());
        assertEquals("toonverse", result.content().getFirst().value().path("source").asString());
        assertEquals("series-1", result.content().getFirst().value().path("data").path("id").asString());
    }

    @Test
    void fetchFullReturnsMetadataAndTheCompleteChapterPage() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            if (exchange.getRequestURI().getPath().equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else if (exchange.getRequestURI().getPath().equals("/api/series/slug/solo-leveling")) {
                body = "{\"data\":{\"id\":\"series-1\",\"title\":\"Solo Leveling\"}}".getBytes();
            } else {
                assertEquals("/api/series/series-1/chapters", exchange.getRequestURI().getPath());
                assertEquals("limit=25&offset=50&order=desc", exchange.getRequestURI().getRawQuery());
                body = "{\"success\":true,\"data\":{\"chapters\":[{\"id\":\"chapter-1\"}],\"total\":205,\"limit\":25,\"offset\":50}}".getBytes();
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper);

        var result = tool.fetchFull(new ToonverseFetchFullArgs("Solo Leveling", 25, 50, "desc"), null);

        assertEquals(false, result.isError());
        assertEquals("toonverse", result.content().getFirst().value().path("source").asString());
        assertEquals("series-1", result.content().getFirst().value().path("metadata").path("id").asString());
        assertEquals("chapter-1", result.content().getFirst().value().path("chapterPage").path("chapters").get(0).path("id").asString());
        assertEquals(205, result.content().getFirst().value().path("chapterPage").path("total").asInt());
        assertEquals(25, result.content().getFirst().value().path("chapterPage").path("limit").asInt());
        assertEquals(50, result.content().getFirst().value().path("chapterPage").path("offset").asInt());
    }

    @Test
    void fetchChapterResolvesTheNameAndUsesTheSlugAndNumberReaderRoute() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            if (exchange.getRequestURI().getPath().equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else {
                assertEquals("/api/reading/chapter/solo-leveling/191", exchange.getRequestURI().getPath());
                body = "{\"data\":{\"chapter\":{\"id\":\"chapter-191\",\"number\":191}}}".getBytes();
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper);

        var result = tool.fetchChapter(new ToonverseFetchChapterArgs("Solo Leveling", 191), null);

        assertEquals(false, result.isError());
        assertEquals("chapter-191", result.content().getFirst().value().path("data").path("chapter").path("id").asString());
    }

    @Test
    void fetchChaptersResolvesOnceAndRetrievesTheInclusiveChapterRange() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            if (exchange.getRequestURI().getPath().equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else {
                String path = exchange.getRequestURI().getPath();
                assertEquals(true, path.equals("/api/reading/chapter/solo-leveling/191") || path.equals("/api/reading/chapter/solo-leveling/192"));
                int chapterNumber = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                body = ("{\"data\":{\"chapter\":{\"number\":" + chapterNumber + "}}}").getBytes();
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper);

        var result = tool.fetchChapters(new ToonverseFetchChaptersArgs("Solo Leveling", 191, 192), null);

        assertEquals(false, result.isError());
        assertEquals(2, result.content().getFirst().value().path("data").size());
        assertEquals(191, result.content().getFirst().value().path("data").get(0).path("chapter").path("number").asInt());
        assertEquals(192, result.content().getFirst().value().path("data").get(1).path("chapter").path("number").asInt());
    }

    @Test
    void fetchChaptersAcceptsAnInclusiveHundredChapterRange() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger readerRequests = new AtomicInteger();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            if (path.equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else {
                int chapterNumber = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                readerRequests.incrementAndGet();
                body = ("{\"data\":{\"chapter\":{\"number\":" + chapterNumber + "}}}").getBytes();
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper);

        var result = tool.fetchChapters(new ToonverseFetchChaptersArgs("Solo Leveling", 0, 100), null);

        assertEquals(false, result.isError());
        assertEquals(101, result.content().getFirst().value().path("data").size());
        assertEquals(0, result.content().getFirst().value().path("data").get(0).path("chapter").path("number").asInt());
        assertEquals(100, result.content().getFirst().value().path("data").get(100).path("chapter").path("number").asInt());
        assertEquals(101, readerRequests.get());
    }

    @Test
    void downloadBookWritesCoverPageAndBookDescriptorsIntoOneWorkspace() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            String mime = "application/json";
            if (path.equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else if (path.equals("/api/reading/chapter/solo-leveling/191")) {
                body = ("{\"data\":{\"chapter\":{\"number\":191,\"title\":\"Chapter 191\",\"pages\":[{\"number\":1,\"imageUrl\":\"http://127.0.0.1:" + server.getAddress().getPort() + "/media/001.webp\",\"width\":720,\"height\":1000,\"hidden\":false}]},\"series\":{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\",\"coverUrl\":\"http://127.0.0.1:" + server.getAddress().getPort() + "/media/cover.webp\",\"type\":\"manhwa\",\"genres\":[\"Action\"]}}}").getBytes();
            } else if (path.equals("/media/001.webp") || path.equals("/media/cover.webp")) {
                body = "webp".getBytes(); mime = "image/webp";
            } else {
                throw new AssertionError("Unexpected path: " + path);
            }
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        RecordingStorage storage = new RecordingStorage();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper, storage);
        List<ProgressUpdate> events = new ArrayList<>();

        var result = tool.downloadBook(
                new ToonverseDownloadBookArgs("Solo Leveling", 191, 191, 120, 10),
                new McpCallContext(null, null, "session-1", "caller-request"),
                McpProgressReporter.webSocket(events::add)
        );

        assertEquals(false, result.isError());
        assertEquals(true, storage.published);
        assertEquals(true, storage.files.containsKey("cover.webp"));
        assertEquals(true, storage.files.containsKey("chapters/0191/001.webp"));
        assertEquals(true, storage.files.containsKey("chapters/0191/chapter.json"));
        var book = objectMapper.readTree(storage.files.get("book.json"));
        assertEquals("open-ink.book/v1", book.path("type").asString());
        assertEquals("cover.webp", book.path("cover").asString());
        assertEquals("chapters/0191/chapter.json", book.path("chapters").get(0).path("path").asString());
        assertEquals(McpProgressReporter.UpdateState.PLANNED, events.getFirst().state());
        assertEquals(1, events.getFirst().total());
        assertEquals(Duration.ofMillis(2_500), events.getFirst().estimate());
        assertEquals("downloading", events.get(3).phase());
        assertEquals(1, events.get(3).unitsCompleted());
        assertEquals(McpProgressReporter.UpdateState.COMPLETED_SUCCESSFUL, events.getLast().state());
        assertEquals(1, events.getLast().unitsCompleted());
    }

    @Test
    void downloadBookDelegatesMediaUrlsWithoutOpeningMediaStreams() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            if (path.equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else if (path.equals("/api/reading/chapter/solo-leveling/191")) {
                body = ("{\"data\":{\"chapter\":{\"number\":191,\"pages\":[{\"number\":1,\"imageUrl\":\"https://images.example.test/page.webp\",\"hidden\":false}]},\"series\":{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\",\"coverUrl\":\"https://images.example.test/cover.webp\"}}}").getBytes();
            } else {
                throw new AssertionError("Delegated media must not be requested by Meshingress: " + path);
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        DelegatingStorage storage = new DelegatingStorage();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper, storage);

        var result = tool.downloadBook(new ToonverseDownloadBookArgs("Solo Leveling", 191, 191, 120, 10),
                new McpCallContext(null, null, "session-1", "caller-request"), McpProgressReporter.NOOP);

        assertEquals(false, result.isError());
        assertEquals(List.of("chapters/0191/001.webp", "cover.webp"), storage.delegatedPaths);
        assertEquals(false, storage.files.containsKey("chapters/0191/001.webp"));
        assertEquals(true, storage.files.containsKey("chapters/0191/chapter.json"));
        assertEquals(true, storage.files.containsKey("book.json"));
    }

    @Test
    void downloadBookRejectsUnpublishedChapterNumbersBeforeOpeningStorage() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger readerRequests = new AtomicInteger();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            if (path.equals("/api/series/search")) {
                body = "{\"data\":[{\"title\":\"Solo Leveling\",\"slug\":\"solo-leveling\"}]}".getBytes();
            } else if (path.equals("/api/reading/chapter/solo-leveling/1")) {
                readerRequests.incrementAndGet();
                body = "{\"data\":{\"chapter\":{\"number\":1,\"pages\":[]},\"chapterNumbers\":[2,1,0]}}".getBytes();
            } else {
                throw new AssertionError("Unexpected path: " + path);
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        RecordingStorage storage = new RecordingStorage();
        ToonverseTool tool = new ToonverseTool(client(), objectMapper, storage);
        List<ProgressUpdate> events = new ArrayList<>();

        var result = tool.downloadBook(
                new ToonverseDownloadBookArgs("Solo Leveling", 1, 3, 120, 10),
                new McpCallContext(null, null, "session-1", "caller-request"),
                McpProgressReporter.webSocket(events::add)
        );

        assertEquals(true, result.isError());
        assertEquals(1, readerRequests.get());
        assertEquals(null, storage.workspace);
        assertEquals(McpProgressReporter.UpdateState.PLANNED, events.getFirst().state());
        assertEquals(McpProgressReporter.UpdateState.COMPLETED_FAILED, events.getLast().state());
        assertEquals(0, events.getLast().unitsCompleted());
    }

    @Test
    void reflectionDiscoversOnlyTheFetchAndSearchSourceOperations() {
        var annotation = new McpToolAnnotationScanner(objectMapper).scan(ToonverseTool.class);

        assertEquals("toonverse", annotation.descriptor().name());
        assertEquals(java.util.Set.of("toonverse.fetch", "toonverse.fetch-full", "toonverse.fetch-chapter", "toonverse.fetch-chapters", "toonverse.download-book", "toonverse.search"),
                annotation.functions().stream().map(function -> function.descriptor().name()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(900_000L, annotation.functions().stream()
                .filter(function -> function.descriptor().name().equals("toonverse.download-book"))
                .findFirst().orElseThrow().descriptor().annotations().path("timeoutMs").asLong());
    }

    private ToonverseClient client() {
        ToonverseSourceConfiguration configuration = new ToonverseSourceConfiguration(
                "toonverse", "Toonverse", URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                "/api/series/slug/{slug}", "$.data", "/api/series/search", "$.data", "/api/series", "$.data",
                "/api/reading/chapter/{slug}/{chapterNumber}", "$.data",
                "/api/series/{seriesId}/chapters", "$.data.chapters", Duration.ofSeconds(2),
                ToonverseSourceConfiguration.AuthorizationMode.NONE, 20, 100, 50, 500, "asc"
        );
        return new ToonverseClient(HttpClient.newHttpClient(), objectMapper, configuration);
    }

    private static class RecordingStorage implements ToolStorageService {
        protected final Map<String, byte[]> files = new LinkedHashMap<>();
        private ToolStorageWorkspace workspace;
        private boolean published;

        @Override
        public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
            OffsetDateTime now = OffsetDateTime.now();
            workspace = new ToolStorageWorkspace(context.sessionId(), "req-test", toolId, "/storage/" + context.sessionId() + "/req-test/files/", now, now.plusMinutes(2), 10, false);
            return workspace;
        }

        @Override
        public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
            try {
                byte[] bytes = content.readAllBytes();
                files.put(relativePath, bytes);
                return new ToolStorageFile(relativePath, workspace.filesUri() + relativePath, request.mimeType(), bytes.length, "checksum");
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public ToolStorageWorkspace publish(ToolStorageWorkspace workspace) {
            published = true;
            return new ToolStorageWorkspace(workspace.sessionId(), workspace.requestId(), workspace.toolId(), workspace.filesUri(), workspace.createdAt(), workspace.expiresAt(), workspace.remainingRequests(), true);
        }

    }

    private static final class DelegatingStorage extends RecordingStorage {
        private final List<String> delegatedPaths = new ArrayList<>();

        @Override
        public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
            assertEquals(ToolStorageTransferMode.DELEGATED_SOURCE_URLS, request.transferMode());
            OffsetDateTime now = OffsetDateTime.now();
            return new ToolStorageWorkspace(context.sessionId(), "req-test", toolId, "/storage/delegated/token/files/", now, now.plusMinutes(2), 10, false,
                    "OPEN", ToolStorageTransferMode.DELEGATED_SOURCE_URLS, null);
        }

        @Override public dev.mrk.meshingress.api.storage.ToolStorageDelegatedFile delegateFile(ToolStorageWorkspace workspace, URI sourceUrl, String relativePath) {
            delegatedPaths.add(relativePath);
            return new dev.mrk.meshingress.api.storage.ToolStorageDelegatedFile(relativePath, sourceUrl);
        }
    }
}
