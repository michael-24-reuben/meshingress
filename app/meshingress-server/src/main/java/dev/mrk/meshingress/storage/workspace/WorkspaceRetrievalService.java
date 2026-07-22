package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.config.MeshingressProperties;

import java.io.InputStream;
import java.time.OffsetDateTime;

public final class WorkspaceRetrievalService {
    public record OpenFile(WorkspaceFileRecord file, InputStream input, Runnable close) { }
    private final MeshingressProperties.Storage properties;
    private final WorkspaceMetadataStore metadata;
    private final WorkspaceFiles files;
    private final WorkspacePathLayout paths;

    public WorkspaceRetrievalService(MeshingressProperties.Storage properties, WorkspaceMetadataStore metadata, WorkspaceFiles files, WorkspacePathLayout paths) {
        this.properties = properties; this.metadata = metadata; this.files = files; this.paths = paths;
    }

    public WorkspaceFileRecord inspect(String sessionId, String requestId, String relativePath) {
        String path = paths.relativePath(relativePath);
        if (path.equals("manifest.json")) {
            metadata.inspect(sessionId, requestId, OffsetDateTime.now()).orElseThrow(() -> new ToolStorageException("Storage file is unavailable."));
            return new WorkspaceFileRecord(path, "application/json", size(sessionId, requestId, path), null);
        }
        return metadata.inspectFile(sessionId, requestId, path, OffsetDateTime.now()).orElseThrow(() -> new ToolStorageException("Storage file is unavailable."));
    }

    public OpenFile open(String sessionId, String requestId, String relativePath) {
        String path = paths.relativePath(relativePath);
        WorkspaceFileRecord admitted = metadata.admitFile(sessionId, requestId, path, OffsetDateTime.now(), properties.local().published().maxConcurrentRetrievals()).orElseThrow(() -> new ToolStorageException("Storage file is unavailable."));
        try {
            WorkspaceFileRecord file = path.equals("manifest.json") ? new WorkspaceFileRecord(path, "application/json", size(sessionId, requestId, path), null) : admitted;
            InputStream input = files.open(sessionId, requestId, path);
            return new OpenFile(file, input, () -> metadata.completeStream(sessionId, requestId));
        } catch (Exception exception) {
            metadata.completeStream(sessionId, requestId);
            throw new ToolStorageException("Storage file is unavailable.", exception);
        }
    }
    private long size(String sessionId, String requestId, String path) { try { return files.size(sessionId, requestId, path); } catch (Exception exception) { throw new ToolStorageException("Storage file is unavailable.", exception); } }
}
