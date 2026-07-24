package dev.mrk.meshingress.api.storage;

import java.time.Duration;

/**
 * The tool-owned contract used when opening a storage workspace.
 *
 * <p>{@code transferMode} chooses who obtains the bytes.  A local publication
 * mode is meaningful only for {@link ToolStorageTransferMode#LOCAL_BYTES}; a
 * delegated destination owns its own import scheduling and must not be given a
 * Meshingress handoff mode.</p>
 */
public record ToolStorageWorkspaceRequest(
        Duration ttl,
        Integer maxRequests,
        ToolStorageTransferMode transferMode,
        ToolStorageLocalPublicationMode localPublicationMode
) {
    public ToolStorageWorkspaceRequest {
        transferMode = transferMode == null ? ToolStorageTransferMode.LOCAL_BYTES : transferMode;
        if (transferMode == ToolStorageTransferMode.LOCAL_BYTES && localPublicationMode == null) {
            localPublicationMode = ToolStorageLocalPublicationMode.INLINE;
        }
    }

    /** Preserves the original local-byte, inline-publication default. */
    public ToolStorageWorkspaceRequest(Duration ttl, Integer maxRequests) {
        this(ttl, maxRequests, ToolStorageTransferMode.LOCAL_BYTES, ToolStorageLocalPublicationMode.INLINE);
    }

    public ToolStorageWorkspaceRequest(Duration ttl, Integer maxRequests, ToolStorageTransferMode transferMode) {
        this(ttl, maxRequests, transferMode,
                transferMode == ToolStorageTransferMode.LOCAL_BYTES ? ToolStorageLocalPublicationMode.INLINE : null);
    }
}
