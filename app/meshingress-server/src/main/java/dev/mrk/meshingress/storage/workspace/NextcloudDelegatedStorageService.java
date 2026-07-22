package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.*;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Storage adapter that keeps only native tool bytes in transit and delegates HTTPS media to Nextcloud. */
public final class NextcloudDelegatedStorageService implements ToolStorageService {
    private final NextcloudDelegatedWorkspaceClient client;

    public NextcloudDelegatedStorageService(NextcloudDelegatedWorkspaceClient client) { this.client = client; }

    @Override public ToolStorageTransferMode transferMode() { return ToolStorageTransferMode.DELEGATED_SOURCE_URLS; }

    @Override
    public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
        if (toolId == null || toolId.isBlank()) throw new ToolStorageException("A tool ID is required.");
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        String sessionId = context == null || context.sessionId() == null ? "" : context.sessionId();
        JsonNode remote = client.reserve(toolId.trim(), sessionId, requestId);
        return workspace(remote, toolId.trim(), sessionId, requestId, false);
    }

    @Override
    public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
        requireOpen(workspace);
        if (request == null || request.mimeType() == null || request.mimeType().isBlank()) throw new ToolStorageException("A file MIME type is required.");
        JsonNode stored = client.upload(workspace.requestId(), workspace.toolId(), relativePath, content, request.mimeType());
        return new ToolStorageFile(relativePath, stored.path("uri").asText(workspace.filesUri() + relativePath), request.mimeType(), stored.path("byteSize").asLong(0), stored.path("checksumSha256").asText(""));
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
        return workspace(client.seal(workspace.requestId(), workspace.toolId()), workspace.toolId(), workspace.sessionId(), workspace.requestId(), true);
    }

    @Override
    public ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
        JsonNode status = client.status(requestId);
        return new ToolStoragePublicationStatus(sessionId, requestId, status.path("state").asText("UNKNOWN"), "nextcloud", status.path("attempts").asInt(0), null, status.path("error").isNull() ? null : status.path("error").asText(null));
    }

    private static void requireOpen(ToolStorageWorkspace workspace) {
        if (workspace == null || workspace.published()) throw new ToolStorageException("An open delegated storage workspace is required.");
    }

    private static ToolStorageWorkspace workspace(JsonNode remote, String toolId, String sessionId, String requestId, boolean published) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ToolStorageWorkspace(sessionId, remote.path("workspaceId").asText(requestId), toolId,
                remote.path("workspaceUri").asText(""), now, now.plusDays(1), 1, published, remote.path("state").asText(published ? "QUEUED" : "OPEN"));
    }
}
