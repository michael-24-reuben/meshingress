package dev.mrk.meshingress.api.storage;

import java.time.OffsetDateTime;

/** Public description of a tool's output workspace. */
public record ToolStorageWorkspace(
        String sessionId,
        String requestId,
        String toolId,
        String filesUri,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        int remainingRequests,
        boolean published,
        String publicationState,
        ToolStorageTransferMode transferMode,
        ToolStorageLocalPublicationMode localPublicationMode
) {
    public ToolStorageWorkspace {
        transferMode = transferMode == null ? ToolStorageTransferMode.LOCAL_BYTES : transferMode;
        if (transferMode == ToolStorageTransferMode.LOCAL_BYTES && localPublicationMode == null) {
            localPublicationMode = ToolStorageLocalPublicationMode.INLINE;
        }
    }

    public ToolStorageWorkspace(String sessionId, String requestId, String toolId, String filesUri,
                                OffsetDateTime createdAt, OffsetDateTime expiresAt, int remainingRequests, boolean published) {
        this(sessionId, requestId, toolId, filesUri, createdAt, expiresAt, remainingRequests, published,
                published ? "AVAILABLE" : "STAGING", ToolStorageTransferMode.LOCAL_BYTES, ToolStorageLocalPublicationMode.INLINE);
    }

    /** Source-compatible constructor for callers that already supplied a state. */
    public ToolStorageWorkspace(String sessionId, String requestId, String toolId, String filesUri,
                                OffsetDateTime createdAt, OffsetDateTime expiresAt, int remainingRequests, boolean published,
                                String publicationState) {
        this(sessionId, requestId, toolId, filesUri, createdAt, expiresAt, remainingRequests, published,
                publicationState, ToolStorageTransferMode.LOCAL_BYTES, ToolStorageLocalPublicationMode.INLINE);
    }
}
