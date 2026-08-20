package dev.mrk.meshingress.api;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** A credential-safe, server-verified caller identity. */
public record McpPrincipal(
        String subject,
        String issuer,
        McpAuthenticationMethod authenticationMethod,
        Instant expiresAt,
        Set<String> roles,
        Set<String> grants,
        String profileId,
        String tenantId
) {
    public McpPrincipal {
        subject = text(subject, "anonymous");
        issuer = text(issuer, "");
        authenticationMethod = Objects.requireNonNullElse(authenticationMethod, McpAuthenticationMethod.ANONYMOUS);
        roles = normalized(roles);
        grants = normalized(grants);
        profileId = text(profileId, "");
        tenantId = text(tenantId, "");
    }

    public static McpPrincipal anonymous() {
        return new McpPrincipal("anonymous", "", McpAuthenticationMethod.ANONYMOUS, null, Set.of(), Set.of(), "", "");
    }

    public boolean authenticated() {
        return authenticationMethod != McpAuthenticationMethod.ANONYMOUS;
    }

    public boolean hasRole(String role) {
        return role != null && roles.contains(role.trim().toLowerCase(java.util.Locale.ROOT));
    }

    /** Returns the same verified principal with server-selected local profile ownership. */
    public McpPrincipal withProfile(String nextProfileId, String nextTenantId) {
        return new McpPrincipal(subject, issuer, authenticationMethod, expiresAt, roles, grants, nextProfileId, nextTenantId);
    }

    private static Set<String> normalized(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(java.util.Locale.ROOT)).forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
