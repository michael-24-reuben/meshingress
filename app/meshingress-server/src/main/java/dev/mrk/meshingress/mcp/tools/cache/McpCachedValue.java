package dev.mrk.meshingress.mcp.tools.cache;

import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Objects;

public record McpCachedValue(
        Instant createdAt,
        Instant expiresAt,
        ObjectNode result
) {
    public McpCachedValue {
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
