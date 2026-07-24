package dev.mrk.meshingress.api.storage;

import dev.mrk.meshingress.api.McpCallContext;

import java.io.InputStream;
import java.net.URI;

/*#architect: version-2 should conceptually support offloading URLs via a delegated external service. This way
 * the Meshingress server can delegate the storage of files to a more appropriate and scalable solution. Eliminating
 * the need to configure the tool to define how to download locally as well as to external storages
 */

/**
 * Tool-facing API for a short-lived, session-scoped output workspace.
 * Implemented by the Meshingress server; tools never receive a filesystem path.
 */
public interface ToolStorageService {

    ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request);

    ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request);

    /**
     * Registers a source that the delegated destination will fetch after sealing.
     * Implementations must reject this call unless the workspace was opened with
     * {@link ToolStorageTransferMode#DELEGATED_SOURCE_URLS}.
     */
    default ToolStorageDelegatedFile delegateFile(ToolStorageWorkspace workspace, URI sourceUrl, String relativePath) {
        throw new UnsupportedOperationException("This storage implementation does not support delegated source URLs.");
    }

    ToolStorageWorkspace publish(ToolStorageWorkspace workspace);

    default ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
        throw new UnsupportedOperationException("This storage implementation does not expose publication status.");
    }

    /** Requeues a retained terminal async handoff after an operator has addressed its cause. */
    default ToolStoragePublicationStatus retryPublication(String sessionId, String requestId) {
        throw new UnsupportedOperationException("This storage implementation does not support publication retry.");
    }
}
