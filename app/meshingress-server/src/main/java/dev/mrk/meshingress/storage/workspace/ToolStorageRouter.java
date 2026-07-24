package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageDelegatedFile;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageTransferMode;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * Routes each workspace by its tool-requested transfer contract rather than by
 * the process-wide local-byte lifecycle. Delegated identity is persisted in the
 * viewer-capability store, so status calls remain correctly routed after restart.
 */
public final class ToolStorageRouter implements ToolStorageService {
    private final ToolStorageService localBytes;
    private final Optional<ToolStorageService> delegatedSources;
    private final Optional<DelegatedViewerCapabilityStore> delegatedWorkspaces;

    public ToolStorageRouter(ToolStorageService localBytes, ToolStorageService delegatedSources,
                             DelegatedViewerCapabilityStore delegatedWorkspaces) {
        this.localBytes = localBytes;
        this.delegatedSources = Optional.ofNullable(delegatedSources);
        this.delegatedWorkspaces = Optional.ofNullable(delegatedWorkspaces);
    }

    @Override
    public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
        ToolStorageWorkspaceRequest resolved = request == null ? new ToolStorageWorkspaceRequest(null, null) : request;
        if (resolved.transferMode() == ToolStorageTransferMode.LOCAL_BYTES) return localBytes.openWorkspace(toolId, context, resolved);
        if (resolved.localPublicationMode() != null)
            throw new ToolStorageException("Delegated source URLs cannot use a Meshingress local publication mode.");
        return delegated().openWorkspace(toolId, context, resolved);
    }

    @Override
    public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
        return service(workspace).writeFile(workspace, relativePath, content, request);
    }

    @Override
    public ToolStorageDelegatedFile delegateFile(ToolStorageWorkspace workspace, URI sourceUrl, String relativePath) {
        if (!delegated(workspace))
            throw new ToolStorageException("Only a DELEGATED_SOURCE_URLS workspace can register source URLs.");
        return delegated().delegateFile(workspace, sourceUrl, relativePath);
    }

    @Override
    public ToolStorageWorkspace publish(ToolStorageWorkspace workspace) {
        return service(workspace).publish(workspace);
    }

    @Override
    public ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
        return delegated(requestId) ? delegated().publicationStatus(sessionId, requestId) : localBytes.publicationStatus(sessionId, requestId);
    }

    @Override
    public ToolStoragePublicationStatus retryPublication(String sessionId, String requestId) {
        return delegated(requestId) ? delegated().retryPublication(sessionId, requestId) : localBytes.retryPublication(sessionId, requestId);
    }

    private ToolStorageService service(ToolStorageWorkspace workspace) {
        if (workspace == null) throw new ToolStorageException("A storage workspace is required.");
        return delegated(workspace) ? delegated() : localBytes;
    }

    private boolean delegated(ToolStorageWorkspace workspace) {
        return workspace.transferMode() == ToolStorageTransferMode.DELEGATED_SOURCE_URLS || delegated(workspace.requestId());
    }

    private boolean delegated(String requestId) {
        return delegatedWorkspaces.map(store -> store.findByRequest(requestId).isPresent()).orElse(false);
    }

    private ToolStorageService delegated() {
        return delegatedSources.orElseThrow(() -> new ToolStorageException("No delegated-source destination is configured."));
    }
}
