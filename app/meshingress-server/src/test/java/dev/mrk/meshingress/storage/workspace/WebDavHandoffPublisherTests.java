package dev.mrk.meshingress.storage.workspace;

import com.sun.net.httpserver.HttpServer;
import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebDavHandoffPublisherTests {
    @TempDir Path temp;

    @Test
    void handsOffWithConditionalCreatesThenRetainsOnlyLocalAuditMetadata() throws Exception {
        List<String> methods = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<String> requestPaths = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/handoff", exchange -> {
            methods.add(exchange.getRequestMethod());
            conditions.add(exchange.getRequestHeaders().getFirst("If-None-Match"));
            requestPaths.add(exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            String schema = "handoff";
            MeshingressProperties.Storage.Target target = new MeshingressProperties.Storage.Target(MeshingressProperties.Storage.Provider.WEBDAV, true,
                    "http://localhost:" + server.getAddress().getPort(), "/handoff", "", "", "", MeshingressProperties.Storage.StagingMode.DIRECT_FINAL_ONLY);
            MeshingressProperties.Storage storage = new MeshingressProperties.Storage(true, MeshingressProperties.Storage.Lifecycle.LOCAL_EXTERNAL, DataSize.ofMegabytes(1),
                    new MeshingressProperties.Storage.Local(temp.toString(), 4, new MeshingressProperties.Storage.Staging(DataSize.ofMegabytes(1), Duration.ofMinutes(1)),
                            new MeshingressProperties.Storage.Published(DataSize.ofMegabytes(1), Duration.ofMinutes(1), Duration.ofHours(1), 1, 1, 1), new MeshingressProperties.Storage.Cleanup(Duration.ofMinutes(1), 10)),
                    new MeshingressProperties.Storage.External("dav", MeshingressProperties.Storage.AccessMode.WRITE_ONLY, MeshingressProperties.Storage.MutationPolicy.CREATE_ONLY,
                            MeshingressProperties.Storage.ConflictPolicy.FAIL, MeshingressProperties.Storage.RetentionPolicy.PROVIDER_MANAGED, 1, Duration.ofSeconds(5),
                            new MeshingressProperties.Storage.AsyncHandoff(Duration.ofSeconds(1), Duration.ofSeconds(10), 3, Duration.ofMillis(10), Duration.ofSeconds(1)), Map.of("dav", target)),
                    new MeshingressProperties.Storage.Metadata(new MeshingressProperties.Storage.Sql(schema, "storage_", new MeshingressProperties.Storage.Tables("entries", "usage", "events"), true)));
            JdbcDataSource source = new JdbcDataSource(); source.setURL("jdbc:h2:mem:" + schema + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            WorkspacePathLayout paths = new WorkspacePathLayout(storage.local().root());
            WorkspaceFiles files = new WorkspaceFiles(paths);
            WorkspaceMetadataStore metadata = new WorkspaceMetadataStore(new JdbcTemplate(source), new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(source)), storage.metadata().sql());
            ToolWorkspaceStorageService service = new ToolWorkspaceStorageService(storage, metadata, files, paths, new ObjectMapper(), new WebDavHandoffPublisher("dav", target, Duration.ofSeconds(5)));

            ToolStorageWorkspace staging = service.openWorkspace("tool", new McpCallContext(null, null, "session-1", "request-1"), new ToolStorageWorkspaceRequest(null, 1));
            service.writeFile(staging, "chapters/output.txt", new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), new ToolStorageFileRequest("text/plain"));
            ToolStorageWorkspace handedOff = service.publish(staging);

            assertEquals("", handedOff.filesUri());
            assertEquals(List.of("MKCOL", "MKCOL", "MKCOL", "MKCOL", "PUT", "PUT"), methods);
            assertTrue(conditions.subList(0, 4).stream().allMatch(condition -> condition == null));
            assertEquals(List.of("*", "*"), conditions.subList(4, 6));
            assertEquals(List.of(
                    "/handoff/session-1/",
                    "/handoff/session-1/" + staging.requestId() + "/",
                    "/handoff/session-1/" + staging.requestId() + "/files/",
                    "/handoff/session-1/" + staging.requestId() + "/files/chapters/",
                    "/handoff/session-1/" + staging.requestId() + "/files/chapters/output.txt",
                    "/handoff/session-1/" + staging.requestId() + "/files/manifest.json"), requestPaths);
            assertEquals(1, new JdbcTemplate(source).queryForObject("select count(*) from " + schema + ".events where event_type = 'HANDOFF_COMPLETED'", Integer.class));
            assertFalse(temp.resolve(".staging").resolve("session-1").resolve(staging.requestId()).toFile().exists());
        } finally { server.stop(0); }
    }

    @Test
    void reportsAnInvalidOrMissingRemoteBasePathWithoutProbingIt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/handoff", exchange -> { exchange.sendResponseHeaders(404, -1); exchange.close(); });
        server.start();
        try {
            MeshingressProperties.Storage.Target target = new MeshingressProperties.Storage.Target(MeshingressProperties.Storage.Provider.WEBDAV, true,
                    "http://localhost:" + server.getAddress().getPort(), "/handoff", "", "", "", MeshingressProperties.Storage.StagingMode.DIRECT_FINAL_ONLY);
            WorkspaceFiles files = new WorkspaceFiles(new WorkspacePathLayout(temp.toString()));
            files.write("session-1", "request-1", "output.txt", new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), 1024);
            files.write("session-1", "request-1", "manifest.json", new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), 1024);
            ToolStorageWorkspace workspace = new ToolStorageWorkspace("session-1", "request-1", "tool", "", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(1), 1, false);

            var failure = assertThrows(dev.mrk.meshingress.api.storage.ToolStorageException.class,
                    () -> new WebDavHandoffPublisher("dav", target, Duration.ofSeconds(5)).publish(workspace, List.of(new WorkspaceFileRecord("output.txt", "text/plain", 7, "")), files));
            assertTrue(failure.getMessage().contains("generated collection"));
        } finally { server.stop(0); }
    }
}
