package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStorageLocalPublicationMode;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageTransferMode;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.config.MeshingressProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ToolWorkspaceStorageService implements ToolStorageService {
    private static final Pattern MIME = Pattern.compile("^[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+$");
    private final MeshingressProperties.Storage properties;
    private final WorkspaceMetadataStore metadata;
    private final WorkspaceFiles files;
    private final WorkspacePathLayout paths;
    private final ObjectMapper objectMapper;
    private final Optional<ExternalHandoffPublisher> handoffPublisher;

    public ToolWorkspaceStorageService(MeshingressProperties.Storage properties, WorkspaceMetadataStore metadata, WorkspaceFiles files, WorkspacePathLayout paths, ObjectMapper objectMapper, ExternalHandoffPublisher handoffPublisher) {
        this.properties = properties;
        this.metadata = metadata;
        this.files = files;
        this.paths = paths;
        this.objectMapper = objectMapper;
        this.handoffPublisher = Optional.ofNullable(handoffPublisher);
    }

    @Override
    public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
        if (!properties.enabled()) throw new ToolStorageException("Ephemeral storage is disabled.");
        if (request != null && request.transferMode() != ToolStorageTransferMode.LOCAL_BYTES)
            throw new ToolStorageException("Local workspace storage accepts only LOCAL_BYTES workspaces.");
        if (toolId == null || toolId.isBlank()) throw new ToolStorageException("A tool ID is required.");
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        String sessionId = context == null ? null : context.sessionId();
        paths.stagingDirectory(sessionId, requestId); // validates the IDs before metadata is written
        Duration ttl = request == null || request.ttl() == null ? properties.local().published().defaultTtl() : request.ttl();
        if (ttl.isNegative() || ttl.isZero()) throw new ToolStorageException("Workspace TTL must be positive.");
        ttl = ttl.compareTo(properties.local().published().maxTtl()) > 0 ? properties.local().published().maxTtl() : ttl;
        int maxRequests = request == null || request.maxRequests() == null ? properties.local().published().defaultMaxRequests() : request.maxRequests();
        if (maxRequests <= 0) throw new ToolStorageException("Workspace maxRequests must be positive.");
        maxRequests = Math.min(maxRequests, properties.local().published().maxRequests());
        OffsetDateTime created = OffsetDateTime.now();
        WorkspaceRecord workspace = new WorkspaceRecord(sessionId, requestId, toolId.trim(), WorkspaceState.STAGING, 0, maxRequests, 0, created, created.plus(ttl));
        metadata.create(workspace, properties.local().maxEntries());
        return view(workspace, false, publicationMode(request));
    }

    @Override
    public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
        if (workspace == null || workspace.published()) throw new ToolStorageException("An unpublished storage workspace is required.");
        if (!metadata.isStaging(workspace.sessionId(), workspace.requestId())) throw new ToolStorageException("The workspace is not awaiting files.");
        String path = paths.relativePath(relativePath);
        if (path.equals("manifest.json")) throw new ToolStorageException("manifest.json is reserved for Meshingress.");
        String mime = request == null ? null : request.mimeType();
        if (mime == null || !MIME.matcher(mime.trim()).matches()) throw new ToolStorageException("A valid file MIME type is required.");
        long used = metadata.stagingBytes(workspace.sessionId(), workspace.requestId());
        long remaining = Math.min(properties.maxEntrySize().toBytes() - used, properties.local().staging().maxBytes().toBytes() - metadata.totalStagingBytes());
        if (remaining <= 0) throw new ToolStorageException("Workspace content exceeds the configured size limit.");
        try {
            WorkspaceFiles.WrittenFile written = files.write(workspace.sessionId(), workspace.requestId(), path, content, remaining);
            WorkspaceFileRecord record = new WorkspaceFileRecord(path, mime.trim().toLowerCase(java.util.Locale.ROOT), written.byteSize(), written.checksumSha256());
            try {
                metadata.addFile(workspace.sessionId(), workspace.requestId(), record);
            } catch (RuntimeException exception) {
                files.deleteStaging(workspace.sessionId(), workspace.requestId());
                throw exception;
            }
            return file(workspace, record);
        } catch (ToolStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ToolStorageException("The workspace file could not be written.", exception);
        }
    }

    @Override
    public ToolStorageWorkspace publish(ToolStorageWorkspace workspace) {
        if (workspace == null || workspace.published()) throw new ToolStorageException("An unpublished storage workspace is required.");
        try {
            List<WorkspaceFileRecord> entries = metadata.files(workspace.sessionId(), workspace.requestId());
            writeManifest(workspace, entries);
            ToolStorageLocalPublicationMode mode = workspace.localPublicationMode();
            if (properties.lifecycle() == MeshingressProperties.Storage.Lifecycle.LOCAL_LOCAL) {
                if (mode != ToolStorageLocalPublicationMode.INLINE)
                    throw new ToolStorageException("Queued publication requires meshingress.storage.lifecycle=local-external.");
                WorkspaceRecord published = metadata.publish(workspace.sessionId(), workspace.requestId(), properties.local().published().maxBytes().toBytes(), OffsetDateTime.now());
                files.publish(workspace.sessionId(), workspace.requestId());
                return view(published, true, mode);
            }
            ExternalHandoffPublisher publisher = handoffPublisher.orElseThrow(() -> new ToolStorageException("No external handoff publisher is configured."));
            if (mode == ToolStorageLocalPublicationMode.QUEUED) {
                WorkspaceRecord queued = metadata.enqueueHandoff(workspace.sessionId(), workspace.requestId(), publisher.target(), OffsetDateTime.now());
                return view(queued, true, mode);
            }
            ExternalHandoffPublisher.HandoffReceipt receipt = publisher.publish(workspace, entries, files);
            WorkspaceRecord handedOff = metadata.handoff(workspace.sessionId(), workspace.requestId(), receipt, OffsetDateTime.now());
            files.deleteStaging(workspace.sessionId(), workspace.requestId());
            return view(handedOff, true, mode);
        } catch (Exception exception) {
            if (properties.lifecycle() == MeshingressProperties.Storage.Lifecycle.LOCAL_LOCAL)
                metadata.failed(workspace.sessionId(), workspace.requestId());
            else if (workspace.localPublicationMode() == ToolStorageLocalPublicationMode.QUEUED)
                metadata.handoffAttemptFailed(new HandoffJob(workspace.sessionId(), workspace.requestId(), handoffPublisher.map(ExternalHandoffPublisher::target).orElse(""), HandoffJobState.QUEUED, 0, OffsetDateTime.now(), null, null), exception.getMessage(), false, 1, Duration.ZERO, OffsetDateTime.now());
            else
                metadata.handoffFailed(workspace.sessionId(), workspace.requestId(), handoffPublisher.map(ExternalHandoffPublisher::target).orElse(null));
            if (workspace.localPublicationMode() != ToolStorageLocalPublicationMode.QUEUED) {
                try {
                    files.deleteStaging(workspace.sessionId(), workspace.requestId());
                } catch (Exception ignored) {
                }
            }
            if (properties.lifecycle() == MeshingressProperties.Storage.Lifecycle.LOCAL_LOCAL) {
                try {
                    files.deletePublished(workspace.sessionId(), workspace.requestId());
                } catch (Exception ignored) {
                }
            }
            if (exception instanceof ToolStorageException storageException) throw storageException;
            throw new ToolStorageException("The workspace could not be published.", exception);
        }
    }

    @Override
    public ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
        WorkspaceRecord workspace = metadata.workspaceStatus(sessionId, requestId).orElseThrow(() -> new ToolStorageException("Storage workspace is unavailable."));
        return metadata.handoffStatus(sessionId, requestId)
                .map(job -> new ToolStoragePublicationStatus(sessionId, requestId, workspace.state().name(), job.target(), job.attempts(), job.nextAttemptAt(), job.lastError()))
                .orElseGet(() -> new ToolStoragePublicationStatus(sessionId, requestId, workspace.state().name(), "", 0, null, null));
    }

    @Override
    public ToolStoragePublicationStatus retryPublication(String sessionId, String requestId) {
        metadata.requeueFailedHandoff(sessionId, requestId, OffsetDateTime.now());
        return publicationStatus(sessionId, requestId);
    }

    private void writeManifest(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> entries) throws Exception {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("type", "meshingress.tool-storage-manifest/v1");
        manifest.put("sessionId", workspace.sessionId());
        manifest.put("requestId", workspace.requestId());
        manifest.put("toolId", workspace.toolId());

        if (!workspace.filesUri().isBlank())
            manifest.put("filesUri", workspace.filesUri());
        manifest.put("createdAt", workspace.createdAt());
        manifest.put("expiresAt", workspace.expiresAt());
        manifest.put("files", entries.stream().map(file -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("path", file.relativePath());

            if (!workspace.filesUri().isBlank())
                view.put("uri", workspace.filesUri() + file.relativePath());
            view.put("mimeType", file.mimeType());
            view.put("byteSize", file.byteSize());
            view.put("checksumSha256", file.checksumSha256());

            return view;
        }).toList());
        var target = paths.stagingManifest(workspace.sessionId(), workspace.requestId());
        Files.createDirectories(target.getParent());
        try (var out = Files.newOutputStream(target)) {
            objectMapper.writeValue(out, manifest);
        }
    }

    private ToolStorageWorkspace view(WorkspaceRecord workspace, boolean published, ToolStorageLocalPublicationMode publicationMode) {
        String base = properties.lifecycle() == MeshingressProperties.Storage.Lifecycle.LOCAL_LOCAL ? "/storage/" + workspace.sessionId() + "/" + workspace.requestId() + "/files/" : "";
        return new ToolStorageWorkspace(workspace.sessionId(), workspace.requestId(), workspace.toolId(), base, workspace.createdAt(), workspace.expiresAt(), workspace.remainingRequests(), published, workspace.state().name(), ToolStorageTransferMode.LOCAL_BYTES, publicationMode);
    }

    private static ToolStorageLocalPublicationMode publicationMode(ToolStorageWorkspaceRequest request) {
        return request == null || request.localPublicationMode() == null ? ToolStorageLocalPublicationMode.INLINE : request.localPublicationMode();
    }

    private ToolStorageFile file(ToolStorageWorkspace workspace, WorkspaceFileRecord file) {
        return new ToolStorageFile(file.relativePath(), workspace.filesUri() + file.relativePath(), file.mimeType(), file.byteSize(), file.checksumSha256());
    }
}
