package dev.mrk.toolspace.nextcloud.storage;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.*;
import dev.mrk.meshingress.api.tools.annotation.McpService;
import dev.mrk.toolspace.nextcloud.config.NextcloudStorageProperties;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Storage adapter that keeps only native tool bytes in transit and delegates HTTPS media to Nextcloud.
 */
@McpService(
        value = "nextcloud.storage.delegated-workspaces",
        title = "Nextcloud delegated storage",
        description = "Delegated Nextcloud workspace storage for HTTPS source URLs.",
        provides = ToolStorageService.class
)
public final class NextcloudDelegatedStorageService implements ToolStorageService {
    private final NextcloudDelegatedWorkspaceClient client;
    private final DelegatedViewerCapabilityStore viewers;
    private final NextcloudStorageProperties properties;

    public NextcloudDelegatedStorageService(NextcloudDelegatedWorkspaceClient client, DelegatedViewerCapabilityStore viewers, NextcloudStorageProperties properties) {
        this.client = client;
        this.viewers = viewers;
        this.properties = properties;
    }

    public void init(McpCallContext context) {
        ToolStorageService.super.init();
    }

    @Override
    public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
        if (!properties.isEnabled())
            throw new ToolStorageException("Nextcloud delegated storage is disabled.");
        if (request == null || request.transferMode() != ToolStorageTransferMode.DELEGATED_SOURCE_URLS)
            throw new ToolStorageException("The delegated destination requires DELEGATED_SOURCE_URLS.");
        if (request.localPublicationMode() != null)
            throw new ToolStorageException("Delegated source URLs cannot use a Meshingress local publication mode.");
        if (toolId == null || toolId.isBlank())
            throw new ToolStorageException("A tool ID is required.");

        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        String sessionId = context == null || context.sessionId() == null ? "" : context.sessionId();
        JsonNode remote = client.reserve(toolId.trim(), sessionId, requestId);
        Duration ttl = request.ttl() == null ? properties.getDefaultTtl() : request.ttl();

        if (ttl.isNegative() || ttl.isZero())
            throw new ToolStorageException("Workspace TTL must be positive.");

        ttl = ttl.compareTo(properties.getMaxTtl()) > 0 ? properties.getMaxTtl() : ttl;
        int maxRequests = request.maxRequests() == null ? properties.getDefaultMaxRequests() : request.maxRequests();

        if (maxRequests <= 0)
            throw new ToolStorageException("Workspace maxRequests must be positive.");

        maxRequests = Math.min(maxRequests, properties.getMaxRequests());
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);
        DelegatedViewerCapabilityStore.Capability viewer = viewers.issue(requestId, toolId.trim(), expiresAt, maxRequests);

        return workspace(remote, toolId.trim(), sessionId, requestId, false, viewer);
    }

    @Override
    public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
        requireOpen(workspace);
        if (request == null || request.mimeType() == null || request.mimeType().isBlank())
            throw new ToolStorageException("A file MIME type is required.");
        JsonNode stored = client.upload(workspace.requestId(), workspace.toolId(), relativePath, content, request.mimeType());
        return new ToolStorageFile(relativePath, workspace.filesUri() + relativePath, request.mimeType(), stored.path("byteSize").asLong(0), stored.path("checksumSha256").asString(""));
    }

    @Override
    public ToolStorageDelegatedFile delegateFile(ToolStorageWorkspace workspace, URI sourceUrl, String relativePath) {
        requireOpen(workspace);
        if (sourceUrl == null || !"https".equalsIgnoreCase(sourceUrl.getScheme()))
            throw new ToolStorageException("Only HTTPS delegated source URLs are accepted.");
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
        return new ToolStoragePublicationStatus(
                sessionId,
                requestId,
                status.path("state").asString("UNKNOWN"),
                "nextcloud",
                status.path("attempts").asInt(0),
                null,
                status.path("error").isNull() ? null : status.path("error").asString(null)
        );
    }

    private static void requireOpen(ToolStorageWorkspace workspace) {
        if (workspace == null || workspace.published())
            throw new ToolStorageException("An open delegated storage workspace is required.");
    }

    private DelegatedViewerCapabilityStore.Capability viewer(ToolStorageWorkspace workspace) {
        DelegatedViewerCapabilityStore.Capability persisted = viewers.findByRequest(workspace.requestId())
                .orElseThrow(() -> new ToolStorageException("The delegated viewer capability is unavailable."));
        String marker = "/api/v1/storage/delegated/";
        int start = workspace.filesUri().indexOf(marker);
        int end = workspace.filesUri().indexOf("/files/", start + marker.length());
        if (start < 0 || end < 0) throw new ToolStorageException("The delegated viewer capability is unavailable.");
        String token = workspace.filesUri().substring(start + marker.length(), end);
        return new DelegatedViewerCapabilityStore.Capability(token, persisted.requestId(), persisted.toolId(), persisted.expiresAt(), persisted.remainingRequests());
    }

    private ToolStorageWorkspace workspace(JsonNode remote, String toolId, String sessionId, String requestId, boolean published, DelegatedViewerCapabilityStore.Capability viewer) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ToolStorageWorkspace(sessionId, remote.path("workspaceId").asString(requestId), toolId,
                properties.getViewerBaseUri().replaceAll("/+$", "") + "/api/v1/storage/delegated/" + viewer.token() + "/files/", now, viewer.expiresAt(), viewer.remainingRequests(), published,
                remote.path("state").asString(published ? "QUEUED" : "OPEN"), ToolStorageTransferMode.DELEGATED_SOURCE_URLS, null);
    }
}
