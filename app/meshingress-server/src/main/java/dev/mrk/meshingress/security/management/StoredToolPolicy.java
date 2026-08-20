package dev.mrk.meshingress.security.management;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Tenant-scoped, grant-only policy for one exact tool function name. */
public record StoredToolPolicy(
        UUID policyId,
        String tenantId,
        String toolName,
        Effect effect,
        Set<String> requiredGrants,
        int priority,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public enum Effect { ALLOW, DENY }

    public StoredToolPolicy {
        policyId = Objects.requireNonNull(policyId, "policyId");
        tenantId = required(tenantId, "tenantId");
        toolName = required(toolName, "toolName");
        effect = Objects.requireNonNull(effect, "effect");
        if (priority < 0) throw new IllegalArgumentException("priority must be non-negative");
        requiredGrants = normalized(requiredGrants);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt must not be before createdAt");
    }

    private static Set<String> normalized(Set<String> values) {
        Objects.requireNonNull(values, "requiredGrants");
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) result.add(required(value, "requiredGrants entry").toLowerCase(java.util.Locale.ROOT));
        return Set.copyOf(result);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
