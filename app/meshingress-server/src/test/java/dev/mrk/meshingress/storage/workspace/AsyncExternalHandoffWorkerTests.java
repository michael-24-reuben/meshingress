package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.unit.DataSize;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncExternalHandoffWorkerTests {
    @TempDir Path temp;

    @Test
    void publishQueuesImmediatelyAndWorkerPublishesFilesBeforeManifest() {
        RecordingPublisher publisher = new RecordingPublisher();
        Fixture fixture = fixture(publisher, Duration.ofSeconds(1));
        ToolStorageWorkspace workspace = openAndWrite(fixture);

        ToolStorageWorkspace queued = fixture.service.publish(workspace);

        assertTrue(queued.published());
        assertEquals("HANDOFF_QUEUED", queued.publicationState());
        assertTrue(fixture.paths.stagingDirectory(queued.sessionId(), queued.requestId()).toFile().exists());
        assertTrue(publisher.accepted.isEmpty());

        fixture.worker.runBounded();

        assertEquals(List.of("chapters/001.txt", "manifest.json"), publisher.accepted);
        assertEquals(HandoffJobState.COMPLETED, fixture.metadata.handoffStatus(queued.sessionId(), queued.requestId()).orElseThrow().state());
        assertFalse(fixture.paths.stagingDirectory(queued.sessionId(), queued.requestId()).toFile().exists());
        fixture.worker.close();
    }

    @Test
    void retryableFailureRetainsLocalStagingAndSchedulesRecovery() {
        FailingPublisher publisher = new FailingPublisher("External handoff target 'dav' was rejected with HTTP 503.");
        Fixture fixture = fixture(publisher, Duration.ofSeconds(1));
        ToolStorageWorkspace queued = fixture.service.publish(openAndWrite(fixture));

        fixture.worker.runBounded();

        HandoffJob job = fixture.metadata.handoffStatus(queued.sessionId(), queued.requestId()).orElseThrow();
        assertEquals(HandoffJobState.QUEUED, job.state());
        assertEquals(1, job.attempts());
        assertTrue(fixture.paths.stagingDirectory(queued.sessionId(), queued.requestId()).toFile().exists());
        fixture.worker.close();
    }

    @Test
    void terminalConflictRetainsLocalEvidenceAndDoesNotRetry() {
        FailingPublisher publisher = new FailingPublisher("External handoff conflicted with an existing object; no overwrite was attempted.");
        Fixture fixture = fixture(publisher, Duration.ofSeconds(1));
        ToolStorageWorkspace queued = fixture.service.publish(openAndWrite(fixture));

        fixture.worker.runBounded();

        HandoffJob job = fixture.metadata.handoffStatus(queued.sessionId(), queued.requestId()).orElseThrow();
        assertEquals(HandoffJobState.FAILED, job.state());
        assertTrue(fixture.paths.stagingDirectory(queued.sessionId(), queued.requestId()).toFile().exists());
        ToolStoragePublicationStatus failed = fixture.service.publicationStatus(queued.sessionId(), queued.requestId());
        assertEquals("HANDOFF_FAILED", failed.state());
        assertEquals("dav", failed.target());
        assertEquals("HANDOFF_QUEUED", fixture.service.retryPublication(queued.sessionId(), queued.requestId()).state());
        fixture.worker.close();
    }

    @Test
    void expiredWorkerLeaseIsRecoveredByTheNextWorkerRun() {
        RecordingPublisher publisher = new RecordingPublisher();
        Fixture fixture = fixture(publisher, Duration.ZERO);
        ToolStorageWorkspace queued = fixture.service.publish(openAndWrite(fixture));
        assertTrue(fixture.metadata.claimHandoff(OffsetDateTime.now(), Duration.ZERO).isPresent());

        fixture.worker.runBounded();

        assertEquals(HandoffJobState.COMPLETED, fixture.metadata.handoffStatus(queued.sessionId(), queued.requestId()).orElseThrow().state());
        fixture.worker.close();
    }

    private ToolStorageWorkspace openAndWrite(Fixture fixture) {
        ToolStorageWorkspace workspace = fixture.service.openWorkspace("tool", new McpCallContext(null, null, "session-1", "caller-request"), new ToolStorageWorkspaceRequest(null, 1, dev.mrk.meshingress.api.storage.ToolStorageTransferMode.LOCAL_BYTES, dev.mrk.meshingress.api.storage.ToolStorageLocalPublicationMode.QUEUED));
        fixture.service.writeFile(workspace, "chapters/001.txt", new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), new ToolStorageFileRequest("text/plain"));
        return workspace;
    }

    private Fixture fixture(ExternalHandoffPublisher publisher, Duration leaseDuration) {
        String schema = "async" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MeshingressProperties.Storage.AsyncHandoff async = new MeshingressProperties.Storage.AsyncHandoff(Duration.ofSeconds(1), leaseDuration, 3, Duration.ofSeconds(1), Duration.ofMinutes(1));
        MeshingressProperties.Storage properties = new MeshingressProperties.Storage(true, MeshingressProperties.Storage.Lifecycle.LOCAL_EXTERNAL, DataSize.ofMegabytes(1),
                new MeshingressProperties.Storage.Local(temp.resolve(schema).toString(), 10, new MeshingressProperties.Storage.Staging(DataSize.ofMegabytes(1), Duration.ofMinutes(1)),
                        new MeshingressProperties.Storage.Published(DataSize.ofMegabytes(1), Duration.ofMinutes(1), Duration.ofHours(1), 1, 1, 1), new MeshingressProperties.Storage.Cleanup(Duration.ofMinutes(1), 10)),
                new MeshingressProperties.Storage.External("dav", MeshingressProperties.Storage.AccessMode.WRITE_ONLY, MeshingressProperties.Storage.MutationPolicy.CREATE_ONLY,
                        MeshingressProperties.Storage.ConflictPolicy.FAIL, MeshingressProperties.Storage.RetentionPolicy.PROVIDER_MANAGED, 1, Duration.ofSeconds(5), async, Map.of()),
                new MeshingressProperties.Storage.Metadata(new MeshingressProperties.Storage.Sql(schema, "storage_", new MeshingressProperties.Storage.Tables("entries", "usage", "events"), true)));
        JdbcDataSource source = new JdbcDataSource(); source.setURL("jdbc:h2:mem:" + schema + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        WorkspacePathLayout paths = new WorkspacePathLayout(properties.local().root());
        WorkspaceFiles files = new WorkspaceFiles(paths);
        WorkspaceMetadataStore metadata = new WorkspaceMetadataStore(new JdbcTemplate(source), new TransactionTemplate(new DataSourceTransactionManager(source)), properties.metadata().sql());
        ToolWorkspaceStorageService service = new ToolWorkspaceStorageService(properties, metadata, files, paths, new ObjectMapper(), publisher);
        return new Fixture(service, metadata, files, paths, new AsyncExternalHandoffWorker(properties, metadata, files, publisher));
    }

    private record Fixture(ToolWorkspaceStorageService service, WorkspaceMetadataStore metadata, WorkspaceFiles files, WorkspacePathLayout paths, AsyncExternalHandoffWorker worker) { }

    private static final class RecordingPublisher implements ExternalHandoffPublisher {
        private final List<String> accepted = new ArrayList<>();
        @Override public String target() { return "dav"; }
        @Override public boolean supportsExternalStaging() { return false; }
        @Override public HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles) { throw new AssertionError("worker must use progress-aware publication"); }
        @Override public HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles, HandoffProgress progress) {
            for (WorkspaceFileRecord file : files) { accepted.add(file.relativePath()); progress.accepted(file.relativePath()); }
            accepted.add("manifest.json"); progress.accepted("manifest.json");
            return new HandoffReceipt("dav", "test-receipt");
        }
    }

    private static final class FailingPublisher implements ExternalHandoffPublisher {
        private final String message;
        private FailingPublisher(String message) { this.message = message; }
        @Override public String target() { return "dav"; }
        @Override public boolean supportsExternalStaging() { return false; }
        @Override public HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles) { throw new ToolStorageException(message); }
    }
}
