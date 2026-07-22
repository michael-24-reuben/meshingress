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
        String publicationState
) {
    public ToolStorageWorkspace(String sessionId, String requestId, String toolId, String filesUri,
                                OffsetDateTime createdAt, OffsetDateTime expiresAt, int remainingRequests, boolean published) {
        this(sessionId, requestId, toolId, filesUri, createdAt, expiresAt, remainingRequests, published, published ? "AVAILABLE" : "STAGING");
    }
}
