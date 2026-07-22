package dev.mrk.meshingress.api.storage;

import java.time.OffsetDateTime;

/** Durable status for local or external workspace publication. */
public record ToolStoragePublicationStatus(
        String sessionId,
        String requestId,
        String state,
        String target,
        int attempts,
        OffsetDateTime nextAttemptAt,
        String lastError
) { }
