package dev.mrk.toolspace.storage.mcp;

/** Identifies a stored workspace publication operation. */
public record StoragePublicationStatusParams(
        String sessionId,
        String requestId
) {
}
