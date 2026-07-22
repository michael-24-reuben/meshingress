package dev.mrk.meshingress.storage.workspace;

import java.time.OffsetDateTime;

record HandoffJob(String sessionId, String requestId, String target, HandoffJobState state, int attempts,
                  OffsetDateTime nextAttemptAt, OffsetDateTime leaseUntil, String lastError) { }
