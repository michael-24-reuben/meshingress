package dev.mrk.meshingress.storage.mcp;

/** Identifies a stored workspace publication operation. */
public record StoragePublicationStatusParams(
        String sessionId,
        String requestId
) {
}
