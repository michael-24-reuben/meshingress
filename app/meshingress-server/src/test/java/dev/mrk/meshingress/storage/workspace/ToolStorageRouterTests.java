package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageDelegatedFile;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStorageLocalPublicationMode;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageTransferMode;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolStorageRouterTests {
    @Test
    void oneRouterOpensLocalAndDelegatedWorkspacesWithoutChangingLifecycle() {
        RecordingStorage local = new RecordingStorage(ToolStorageTransferMode.LOCAL_BYTES);
        RecordingStorage delegated = new RecordingStorage(ToolStorageTransferMode.DELEGATED_SOURCE_URLS);
        ToolStorageRouter router = new ToolStorageRouter(local, delegated, null);

        ToolStorageWorkspace localWorkspace = router.openWorkspace("qbit.collect", context(),
                new ToolStorageWorkspaceRequest(null, 1, ToolStorageTransferMode.LOCAL_BYTES, ToolStorageLocalPublicationMode.QUEUED));
        ToolStorageWorkspace delegatedWorkspace = router.openWorkspace("toonverse.download-book", context(),
                new ToolStorageWorkspaceRequest(null, 1, ToolStorageTransferMode.DELEGATED_SOURCE_URLS));

        assertEquals(1, local.opened);
        assertEquals(1, delegated.opened);
        assertEquals(ToolStorageLocalPublicationMode.QUEUED, localWorkspace.localPublicationMode());
        assertEquals(ToolStorageTransferMode.DELEGATED_SOURCE_URLS, delegatedWorkspace.transferMode());
        router.delegateFile(delegatedWorkspace, URI.create("https://media.example.test/page.webp"), "page.webp");
        assertEquals(1, delegated.delegated);
    }

    @Test
    void delegatedSourcesRejectMeshingressQueueSelection() {
        ToolStorageRouter router = new ToolStorageRouter(new RecordingStorage(ToolStorageTransferMode.LOCAL_BYTES),
                new RecordingStorage(ToolStorageTransferMode.DELEGATED_SOURCE_URLS), null);

        assertThrows(ToolStorageException.class, () -> router.openWorkspace("toonverse.download-book", context(),
                new ToolStorageWorkspaceRequest(null, 1, ToolStorageTransferMode.DELEGATED_SOURCE_URLS, ToolStorageLocalPublicationMode.QUEUED)));
    }

    private static McpCallContext context() { return new McpCallContext(null, null, "session-1", "request-1"); }

    private static final class RecordingStorage implements ToolStorageService {
        private final ToolStorageTransferMode mode;
        private int opened;
        private int delegated;

        private RecordingStorage(ToolStorageTransferMode mode) { this.mode = mode; }

        @Override
        public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
            opened++;
            return new ToolStorageWorkspace(context.sessionId(), mode == ToolStorageTransferMode.LOCAL_BYTES ? "local-1" : "delegated-1", toolId,
                    "", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(1), 1, false, "OPEN", mode, request.localPublicationMode());
        }

        @Override public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) { throw new UnsupportedOperationException(); }
        @Override public ToolStorageDelegatedFile delegateFile(ToolStorageWorkspace workspace, URI sourceUrl, String relativePath) { delegated++; return new ToolStorageDelegatedFile(relativePath, sourceUrl); }
        @Override public ToolStorageWorkspace publish(ToolStorageWorkspace workspace) { return workspace; }
        @Override public ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) { throw new UnsupportedOperationException(); }
    }
}
