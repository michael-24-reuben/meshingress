package dev.mrk.meshingress.storage.workspace;

import com.sun.net.httpserver.HttpServer;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NextcloudDelegatedWorkspaceClientTests {
    @Test
    void uploadEncodesNativeBytesAsJsonBase64ForNextcloud34() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> toolId = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/req-1/files", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            toolId.set(exchange.getRequestHeaders().getFirst("X-Meshingress-Tool-Id"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"ocs\":{\"meta\":{\"statuscode\":200},\"data\":{\"byteSize\":10}}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            MeshingressProperties.Storage.Target target = new MeshingressProperties.Storage.Target(
                    MeshingressProperties.Storage.Provider.NEXTCLOUD, true,
                    "http://localhost:" + server.getAddress().getPort(), "", "", "", "",
                    MeshingressProperties.Storage.StagingMode.PROVIDER_SESSION);
            NextcloudDelegatedWorkspaceClient client = new NextcloudDelegatedWorkspaceClient(target, Duration.ofSeconds(5), "", new ObjectMapper());
            byte[] source = "{\"book\":1}".getBytes(StandardCharsets.UTF_8);

            client.upload("req-1", "toonverse.download-book", "book.json", new ByteArrayInputStream(source), "application/json");

            JsonNode payload = new ObjectMapper().readTree(requestBody.get());
            assertEquals("PUT", method.get());
            assertEquals("/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/req-1/files", path.get());
            assertEquals("application/json", contentType.get());
            assertEquals("toonverse.download-book", toolId.get());
            assertEquals("book.json", payload.path("path").asText());
            assertEquals("application/json", payload.path("contentType").asText());
            assertEquals(new String(source, StandardCharsets.UTF_8), new String(Base64.getDecoder().decode(payload.path("contentBase64").asText()), StandardCharsets.UTF_8));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadReadsOnlyTheConfiguredWorkspacePathWithServerCredentials() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/remote.php/dav/files/alice/Workspace/Meshingress/storage/toonverse.download-book/req-1/book.json", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"type\":\"open-ink.book/v1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            MeshingressProperties.Storage.Target target = new MeshingressProperties.Storage.Target(
                    MeshingressProperties.Storage.Provider.NEXTCLOUD, true,
                    "http://localhost:" + server.getAddress().getPort(), "/remote.php/dav/files/alice/Workspace/Meshingress/storage", "", "", "",
                    MeshingressProperties.Storage.StagingMode.PROVIDER_SESSION);
            NextcloudDelegatedWorkspaceClient client = new NextcloudDelegatedWorkspaceClient(target, Duration.ofSeconds(5), "Basic server-only", new ObjectMapper());

            NextcloudDelegatedWorkspaceClient.RemoteFile file = client.download("req-1", "toonverse.download-book", "book.json");

            assertEquals("Basic server-only", authorization.get());
            assertEquals("text/plain", file.mimeType());
            assertEquals(27, file.byteSize());
            assertEquals("{\"type\":\"open-ink.book/v1\"}", new String(file.input().readAllBytes(), StandardCharsets.UTF_8));
            file.input().close();
        } finally {
            server.stop(0);
        }
    }
}
