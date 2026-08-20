package dev.mrk.meshingress.controller.roles.registration;

import java.time.OffsetDateTime;

/** Persisted, current duplicate-function decision for a tool namespace. */
public record ToolContributionConflict(
        String namespace,
        String functionName,
        String owningToolId,
        String rejectedToolId,
        OffsetDateTime resolvedAt
) {
    public ToolContributionConflict {
        if (namespace == null || namespace.isBlank() || functionName == null || functionName.isBlank()
                || owningToolId == null || owningToolId.isBlank() || rejectedToolId == null || rejectedToolId.isBlank()) {
            throw new IllegalArgumentException("tool contribution conflict fields must not be blank");
        }
        resolvedAt = resolvedAt == null ? OffsetDateTime.now() : resolvedAt;
    }
}
