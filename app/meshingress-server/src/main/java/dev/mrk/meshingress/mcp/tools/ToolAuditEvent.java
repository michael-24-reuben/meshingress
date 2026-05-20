package dev.mrk.meshingress.mcp.tools;

import java.time.OffsetDateTime;

public record ToolAuditEvent(
        OffsetDateTime at,
        String actor,
        String action,
        String toolName,
        int previousVersion,
        int newVersion,
        long registryVersion,
        String requestId
) {
}
