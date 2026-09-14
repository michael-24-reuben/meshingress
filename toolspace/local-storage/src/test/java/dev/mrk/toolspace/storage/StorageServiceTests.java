package dev.mrk.toolspace.storage;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.toolspace.localstorage.config.LocalStorageProperties;
import dev.mrk.toolspace.storage.workspace.ToolWorkspaceStorageService;
import dev.mrk.toolspace.storage.workspace.WorkspaceCleanupCoordinator;
import dev.mrk.toolspace.storage.workspace.WorkspaceFiles;
import dev.mrk.toolspace.storage.workspace.WorkspaceMetadataStore;
import dev.mrk.toolspace.storage.workspace.WorkspacePathLayout;
import dev.mrk.toolspace.storage.workspace.WorkspaceRetrievalService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTests {
    @TempDir Path temp;

    @Test
    void publishesNamedFilesAndTheGeneratedManifestUnderTheSessionRequestWorkspace() throws Exception {
        Fixture fixture = fixture(DataSize.ofKilobytes(8), 10);
        ToolStorageWorkspace staging = fixture.service.openWorkspace("open-ink-library.toonverse.fetch-chapters", context(), new ToolStorageWorkspaceRequest(Duration.ofMinutes(2), 2));
        assertEquals("session-1", staging.sessionId());
        assertEquals("/api/v1/storage/session-1/" + staging.requestId() + "/files/", staging.filesUri());

        fixture.service.writeFile(staging, "chapters/001.webp", new ByteArrayInputStream("image".getBytes(StandardCharsets.UTF_8)), new ToolStorageFileRequest("image/webp"));
        ToolStorageWorkspace published = fixture.service.publish(staging);

        var file = fixture.retrieval.open(published.sessionId(), published.requestId(), "chapters/001.webp");
        assertArrayEquals("image".getBytes(StandardCharsets.UTF_8), file.input().readAllBytes());
        file.input().close(); file.close().run();
        var manifest = fixture.retrieval.open(published.sessionId(), published.requestId(), "manifest.json");
        String body = new String(manifest.input().readAllBytes(), StandardCharsets.UTF_8);
        manifest.input().close(); manifest.close().run();
        assertTrue(body.contains("chapters/001.webp"));
    }

    @Test
    void headInspectionDoesNotConsumeButEachFileGetConsumesTheWorkspaceRequestBudget() throws Exception {
        Fixture fixture = fixture(DataSize.ofKilobytes(8), 10);
        ToolStorageWorkspace staging = fixture.service.openWorkspace("tool", context(), new ToolStorageWorkspaceRequest(null, 1));
        fixture.service.writeFile(staging, "note.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), new ToolStorageFileRequest("text/plain"));
        ToolStorageWorkspace published = fixture.service.publish(staging);
        assertEquals(4, fixture.retrieval.inspect(published.sessionId(), published.requestId(), "note.txt").byteSize());
        var file = fixture.retrieval.open(published.sessionId(), published.requestId(), "note.txt");
        file.input().close(); file.close().run();
        assertThrows(RuntimeException.class, () -> fixture.retrieval.inspect(published.sessionId(), published.requestId(), "note.txt"));
    }

    @Test
    void cleanupDeletesTheEntireExhaustedWorkspaceAndReleasesItsEntryCapacity() throws Exception {
        Fixture fixture = fixture(DataSize.ofKilobytes(8), 1);
        ToolStorageWorkspace staging = fixture.service.openWorkspace("tool", context(), new ToolStorageWorkspaceRequest(null, 1));
        fixture.service.writeFile(staging, "note.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), new ToolStorageFileRequest("text/plain"));
        ToolStorageWorkspace published = fixture.service.publish(staging);
        var file = fixture.retrieval.open(published.sessionId(), published.requestId(), "note.txt");
        file.input().close(); file.close().run();
        fixture.cleanup.runBounded();
        assertThrows(RuntimeException.class, () -> fixture.retrieval.inspect(published.sessionId(), published.requestId(), "note.txt"));
        fixture.service.openWorkspace("tool", context(), new ToolStorageWorkspaceRequest(null, 1));
    }

    private Fixture fixture(DataSize maxSize, int maxEntries) {
        String schema = "stg" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LocalStorageProperties properties = new LocalStorageProperties(true, LocalStorageProperties.Lifecycle.LOCAL_LOCAL, maxSize,
                new LocalStorageProperties.Local(temp.resolve(schema).toString(), maxEntries,
                        new LocalStorageProperties.Staging(DataSize.ofMegabytes(1), Duration.ofMinutes(1)),
                        new LocalStorageProperties.Published(DataSize.ofMegabytes(2), Duration.ofMinutes(5), Duration.ofHours(1), 1, 3, 2),
                        new LocalStorageProperties.Cleanup(Duration.ofMinutes(1), 10)),
                LocalStorageProperties.External.defaults(),
                new LocalStorageProperties.Metadata(new LocalStorageProperties.Sql(schema, "storage_", new LocalStorageProperties.Tables("entries", "usage", "events"), true)));
        JdbcDataSource source = new JdbcDataSource(); source.setURL("jdbc:h2:mem:" + schema + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        WorkspacePathLayout paths = new WorkspacePathLayout(properties.local().root());
        WorkspaceFiles files = new WorkspaceFiles(paths);
        WorkspaceMetadataStore metadata = new WorkspaceMetadataStore(new JdbcTemplate(source), new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(source)), properties.metadata().sql());
        return new Fixture(new ToolWorkspaceStorageService(properties, metadata, files, paths, new ObjectMapper(), null), new WorkspaceRetrievalService(properties, metadata, files, paths), new WorkspaceCleanupCoordinator(properties, metadata, files));
    }
    private McpCallContext context() { return new McpCallContext(null, null, "session-1", "caller-request"); }
    private record Fixture(ToolWorkspaceStorageService service, WorkspaceRetrievalService retrieval, WorkspaceCleanupCoordinator cleanup) { }
}
