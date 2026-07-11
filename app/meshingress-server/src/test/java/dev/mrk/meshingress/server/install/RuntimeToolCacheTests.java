package dev.mrk.meshingress.server.install;

import com.sun.net.httpserver.HttpServer;
import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactChecksum;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactProvenance;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeToolCacheTests {

    private static final String GROUP_ID = "dev.mrk.tools";
    private static final String ARTIFACT_ID = "sample-module";
    private static final String VERSION = "0.0.1-SNAPSHOT";
    private static final String JAR_NAME = "sample-module.jar";
    private static final String DOWNLOAD_PATH = "/artifact/dev.mrk.tools/sample-module/0.0.1-SNAPSHOT/file";
    private static final String PUBLICATION_PATH = "/artifact/dev.mrk.tools/sample-module/0.0.1-SNAPSHOT/publication";
    private static final String APPLICATION_RESOURCE_PATH = "/artifact/dev.mrk.tools/sample-module/0.0.1-SNAPSHOT/resources/application.yaml";
    private static final String README_RESOURCE_PATH = "/artifact/dev.mrk.tools/sample-module/0.0.1-SNAPSHOT/resources/README.md";
    private static final String MANIFEST_RESOURCE_PATH = "/artifact/dev.mrk.tools/sample-module/0.0.1-SNAPSHOT/resources/tool-manifest.json";

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void installCopiesLocalRepositoryArtifactWhenApiBaseUrlIsBlank() throws Exception {
        byte[] artifactBytes = "local-repository-jar-bytes".getBytes(StandardCharsets.UTF_8);
        Path repositoryJar = tempDir.resolve("repository")
                .resolve("artifacts")
                .resolve("dev/mrk/tools")
                .resolve(ARTIFACT_ID)
                .resolve(VERSION)
                .resolve(JAR_NAME);
        Files.createDirectories(repositoryJar.getParent());
        Files.write(repositoryJar, artifactBytes);
        Path repositoryResources = repositoryJar.getParent().resolve("resources");
        Files.createDirectories(repositoryResources);
        Files.writeString(repositoryResources.resolve("application.yaml"), "meshingress.sample.executable: \"sample-tool\"\n");
        RuntimeToolCache cache = cache("");

        Path cached = cache.install(publication(sha256(artifactBytes)));

        assertThat(cached).isRegularFile();
        assertThat(cached.getFileName().toString()).isEqualTo(JAR_NAME);
        assertThat(Files.readAllBytes(cached)).isEqualTo(artifactBytes);
        assertThat(cached.getParent().resolve("resources/application.yaml"))
                .isRegularFile()
                .content()
                .contains("meshingress.sample.executable");
    }

    @Test
    void installDownloadsArtifactFromRepositoryApiWhenConfigured() throws Exception {
        byte[] artifactBytes = "repository-api-jar-bytes".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> requestedRole = new AtomicReference<>();
        HttpServer server = serverReturning(artifactBytes, requestedRole);
        server.start();
        try {
            RuntimeToolCache cache = cache("http://127.0.0.1:" + server.getAddress().getPort());

            Path cached = cache.install(publication(sha256(artifactBytes)));

            assertThat(cached).isRegularFile();
            assertThat(cached.getFileName().toString()).isEqualTo(JAR_NAME);
            assertThat(Files.readAllBytes(cached)).isEqualTo(artifactBytes);
            assertThat(cached.getParent().resolve("resources/application.yaml"))
                    .isRegularFile()
                    .content()
                    .contains("meshingress.sample.remote");
            assertThat(cached.getParent().resolve("resources/README.md"))
                    .isRegularFile()
                    .content()
                    .contains("# Remote README");
            assertThat(cached.getParent().resolve("resources/tool-manifest.json"))
                    .isRegularFile()
                    .content()
                    .contains("\"toolId\":\"sample.module\"");
            assertThat(requestedRole.get()).isEqualTo("publisher");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void installRejectsRepositoryApiChecksumMismatchWithoutKeepingCacheFile() throws Exception {
        byte[] downloadedBytes = "tampered-api-jar-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = "expected-api-jar-bytes".getBytes(StandardCharsets.UTF_8);
        HttpServer server = serverReturning(downloadedBytes, new AtomicReference<>());
        server.start();
        try {
            RuntimeToolCache cache = cache("http://127.0.0.1:" + server.getAddress().getPort());

            JsonRpcException failure = assertThrows(
                    JsonRpcException.class,
                    () -> cache.install(publication(sha256(expectedBytes)))
            );

            assertThat(failure.code()).isEqualTo(JsonRpcErrorCodes.FORBIDDEN);
            assertThat(failure.getMessage()).isEqualTo("repository artifact checksum mismatch");
            Path targetDirectory = tempDir.resolve("runtime-cache")
                    .resolve("dev/mrk/tools")
                    .resolve(ARTIFACT_ID)
                    .resolve(VERSION);
            if (Files.exists(targetDirectory)) {
                try (var files = Files.list(targetDirectory)) {
                    assertThat(files.toList()).isEmpty();
                }
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchPublicationDownloadsPublicationRecordFromRepositoryApiWhenConfigured() throws Exception {
        ArtifactPublicationRecord publication = publication("0123456789abcdef");
        AtomicReference<String> requestedRole = new AtomicReference<>();
        HttpServer server = serverReturningPublication(publication, requestedRole);
        server.start();
        try {
            RepositoryArtifactFetcher fetcher = new RepositoryArtifactFetcher(
                    properties("http://127.0.0.1:" + server.getAddress().getPort()),
                    objectMapper,
                    HttpClient.newHttpClient()
            );

            ArtifactPublicationRecord fetched = fetcher.fetchPublication(publication.coordinate());

            assertThat(fetched.coordinate()).isEqualTo(publication.coordinate());
            assertThat(fetched.artifactUri()).isEqualTo(publication.artifactUri());
            assertThat(fetched.signatureKeyId()).isEqualTo(publication.signatureKeyId());
            assertThat(requestedRole.get()).isEqualTo("publisher");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchPublicationRequiresRepositoryApiBaseUrl() {
        RepositoryArtifactFetcher fetcher = new RepositoryArtifactFetcher(properties(""));

        JsonRpcException failure = assertThrows(
                JsonRpcException.class,
                () -> fetcher.fetchPublication(new ArtifactCoordinate(GROUP_ID, ARTIFACT_ID, VERSION, null, "jar"))
        );

        assertThat(failure.code()).isEqualTo(JsonRpcErrorCodes.INVALID_PARAMS);
        assertThat(failure.getMessage()).isEqualTo("Repository API base URL is required to fetch publication records.");
    }

    private RuntimeToolCache cache(String apiBaseUrl) {
        MeshingressProperties properties = properties(apiBaseUrl);
        return new RuntimeToolCache(properties, new RepositoryArtifactFetcher(properties));
    }

    private MeshingressProperties properties(String apiBaseUrl) {
        return new MeshingressProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new MeshingressProperties.Repository(
                        tempDir.resolve("repository").toString(),
                        tempDir.resolve("runtime-cache").toString(),
                        "runtime/tool-registrations.json",
                        apiBaseUrl,
                        "publisher",
                        "local-dev-hmac",
                        "dev-repository-signing-key",
                        List.of()
                )
        );
    }

    private HttpServer serverReturning(byte[] body, AtomicReference<String> requestedRole) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(DOWNLOAD_PATH, exchange -> {
            requestedRole.set(exchange.getRequestHeaders().getFirst("X-Repository-Role"));
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext(APPLICATION_RESOURCE_PATH, exchange -> {
            requestedRole.set(exchange.getRequestHeaders().getFirst("X-Repository-Role"));
            byte[] resourceBody = "meshingress.sample.remote: \"remote-tool\"\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, resourceBody.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(resourceBody);
            }
        });
        server.createContext(README_RESOURCE_PATH, exchange -> {
            requestedRole.set(exchange.getRequestHeaders().getFirst("X-Repository-Role"));
            byte[] resourceBody = "# Remote README\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, resourceBody.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(resourceBody);
            }
        });
        server.createContext(MANIFEST_RESOURCE_PATH, exchange -> {
            requestedRole.set(exchange.getRequestHeaders().getFirst("X-Repository-Role"));
            byte[] resourceBody = "{\"schemaVersion\":1,\"toolId\":\"sample.module\",\"properties\":[],\"requirements\":[],\"links\":[],\"readme\":\"\"}\n"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, resourceBody.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(resourceBody);
            }
        });
        return server;
    }

    private HttpServer serverReturningPublication(ArtifactPublicationRecord publication, AtomicReference<String> requestedRole) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(publication);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(PUBLICATION_PATH, exchange -> {
            requestedRole.set(exchange.getRequestHeaders().getFirst("X-Repository-Role"));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        return server;
    }

    private ArtifactPublicationRecord publication(String sha256) {
        return new ArtifactPublicationRecord(
                new ArtifactCoordinate(GROUP_ID, ARTIFACT_ID, VERSION, null, "jar"),
                MeshingressArtifactType.TOOL_MODULE,
                ArtifactTrustStatus.APPROVED_LIMITED,
                "meshingress-repository://artifact/%s/%s/%s/%s".formatted(GROUP_ID, ARTIFACT_ID, VERSION, JAR_NAME),
                ArtifactChecksum.sha256(sha256),
                new ArtifactScopeDeclaration(List.of("USER_WRITE"), List.of("USER_WRITE"), List.of("USER_WRITE"), List.of()),
                new ArtifactAssessmentSummary("clean", List.of("cyclonedx-sbom", "bytecode-scope-scanner"), 0, Map.of()),
                ArtifactProvenance.empty(),
                false,
                OffsetDateTime.parse("2026-06-22T10:00:00-04:00"),
                "test-key",
                "HmacSHA256",
                "signature"
        );
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
