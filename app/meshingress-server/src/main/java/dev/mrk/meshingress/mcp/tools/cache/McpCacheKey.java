package dev.mrk.meshingress.mcp.tools.cache;

import java.util.Objects;

public record McpCacheKey(String namespace, String hash) {

    public McpCacheKey {
        namespace = sanitizeNamespace(namespace);
        hash = Objects.requireNonNull(hash, "hash must not be null");
        if (hash.length() < 2) {
            throw new IllegalArgumentException("hash must contain at least two characters");
        }
    }

    public String hashPrefix() {
        return hash.substring(0, 2);
    }

    private static String sanitizeNamespace(String value) {
        String normalized = value == null || value.isBlank() ? "default" : value.trim();
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
