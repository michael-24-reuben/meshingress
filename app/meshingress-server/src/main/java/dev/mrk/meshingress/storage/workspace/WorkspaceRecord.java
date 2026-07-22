package dev.mrk.meshingress.storage.workspace;

import java.time.OffsetDateTime;

record WorkspaceRecord(String sessionId, String requestId, String toolId, WorkspaceState state, long byteSize,
                       int remainingRequests, int activeStreams, OffsetDateTime createdAt, OffsetDateTime expiresAt) { }
