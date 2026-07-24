package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.*;
import dev.mrk.meshingress.config.MeshingressProperties;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Storage adapter that keeps only native tool bytes in transit and delegates HTTPS media to Nextcloud. */
public final class NextcloudDelegatedStorageService implements ToolStorageService {
    private final NextcloudDelegatedWorkspaceClient client;
    private final DelegatedViewerCapabilityStore viewers;
    private final String viewerBaseUri;
    private final MeshingressProperties.Storage properties;

    public NextcloudDelegatedStorageService(NextcloudDelegatedWorkspaceClient client, DelegatedViewerCapabilityStore viewers, String viewerBaseUri, MeshingressProperties.Storage properties) {
        this.client = client;
        this.viewers = viewers;
        this.viewerBaseUri = viewerBaseUri.replaceAll("/+$", "");
        this.properties = properties;
    }

    @Override
    public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
        if (!properties.enabled()) throw new ToolStorageException("Ephemeral storage is disabled.");
        if (request == null || request.transferMode() != ToolStorageTransferMode.DELEGATED_SOURCE_URLS)
            throw new ToolStorageException("The delegated destination requires DELEGATED_SOURCE_URLS.");
        if (request.localPublicationMode() != null)
            throw new ToolStorageException("Delegated source URLs cannot use a Meshingress local publication mode.");
        if (toolId == null || toolId.isBlank()) throw new ToolStorageException("A tool ID is required.");
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        String sessionId = context == null || context.sessionId() == null ? "" : context.sessionId();
        JsonNode remote = client.reserve(toolId.trim(), sessionId, requestId);
        Duration ttl = request == null || request.ttl() == null ? properties.local().published().defaultTtl() : request.ttl();
        if (ttl.isNegative() || ttl.isZero()) throw new ToolStorageException("Workspace TTL must be positive.");
        ttl = ttl.compareTo(properties.local().published().maxTtl()) > 0 ? properties.local().published().maxTtl() : ttl;
        int maxRequests = request == null || request.maxRequests() == null ? properties.local().published().defaultMaxRequests() : request.maxRequests();
        if (maxRequests <= 0) throw new ToolStorageException("Workspace maxRequests must be positive.");
        maxRequests = Math.min(maxRequests, properties.local().published().maxRequests());
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);
        DelegatedViewerCapabilityStore.Capability viewer = viewers.issue(requestId, toolId.trim(), expiresAt, maxRequests);
        return workspace(remote, toolId.trim(), sessionId, requestId, false, viewer);
    }

    @Override
    public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
        requireOpen(workspace);
        if (request == null || request.mimeType() == null || request.mimeType().isBlank()) throw new ToolStorageException("A file MIME type is required.");
        JsonNode stored = client.upload(workspace.requestId(), workspace.toolId(), relativePath, content, request.mimeType());
        return new ToolStorageFile(relativePath, workspace.filesUri() + relativePath, request.mimeType(), stored.path("byteSize").asLong(0), stored.path("checksumSha256").asText(""));
    }

    @Override
    public ToolStorageDelegatedFile delegateFile(ToolStorageWorkspace workspace, URI sourceUrl, String relativePath) {
        requireOpen(workspace);
        if (sourceUrl == null || !"https".equalsIgnoreCase(sourceUrl.getScheme())) throw new ToolStorageException("Only HTTPS delegated source URLs are accepted.");
        client.appendSources(workspace.requestId(), workspace.toolId(), List.of(Map.of("url", sourceUrl.toString(), "path", relativePath)));
        return new ToolStorageDelegatedFile(relativePath, sourceUrl);
    }

    @Override
    public ToolStorageWorkspace publish(ToolStorageWorkspace workspace) {
        requireOpen(workspace);
        return workspace(client.seal(workspace.requestId(), workspace.toolId()), workspace.toolId(), workspace.sessionId(), workspace.requestId(), true, viewer(workspace));
    }

    @Override
    public ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
        JsonNode status = client.status(requestId);
        return new ToolStoragePublicationStatus(sessionId, requestId, status.path("state").asText("UNKNOWN"), "nextcloud", status.path("attempts").asInt(0), null, status.path("error").isNull() ? null : status.path("error").asText(null));
    }

    private static void requireOpen(ToolStorageWorkspace workspace) {
        if (workspace == null || workspace.published()) throw new ToolStorageException("An open delegated storage workspace is required.");
    }

    private DelegatedViewerCapabilityStore.Capability viewer(ToolStorageWorkspace workspace) {
        DelegatedViewerCapabilityStore.Capability persisted = viewers.findByRequest(workspace.requestId())
                .orElseThrow(() -> new ToolStorageException("The delegated viewer capability is unavailable."));
        String marker = "/storage/delegated/";
        int start = workspace.filesUri().indexOf(marker);
        int end = workspace.filesUri().indexOf("/files/", start + marker.length());
        if (start < 0 || end < 0) throw new ToolStorageException("The delegated viewer capability is unavailable.");
        String token = workspace.filesUri().substring(start + marker.length(), end);
        return new DelegatedViewerCapabilityStore.Capability(token, persisted.requestId(), persisted.toolId(), persisted.expiresAt(), persisted.remainingRequests());
    }

    private ToolStorageWorkspace workspace(JsonNode remote, String toolId, String sessionId, String requestId, boolean published, DelegatedViewerCapabilityStore.Capability viewer) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ToolStorageWorkspace(sessionId, remote.path("workspaceId").asText(requestId), toolId,
                viewerBaseUri + "/storage/delegated/" + viewer.token() + "/files/", now, viewer.expiresAt(), viewer.remainingRequests(), published,
                remote.path("state").asText(published ? "QUEUED" : "OPEN"), ToolStorageTransferMode.DELEGATED_SOURCE_URLS, null);
    }
}
