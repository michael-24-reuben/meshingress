package dev.mrk.aegis;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A normalized identity owned by an authentication profile. It contains only
 * verified claims and safe metadata; it does not retain credential evidence.
 */
public record AuthenticatedIdentity(
        String id,
        URI issuer,
        String subject,
        AuthenticationMethod authenticationMethod,
        Set<String> audiences,
        Set<String> scopes,
        Set<String> roles,
        Instant lastAuthenticatedAt
) {
    public AuthenticatedIdentity {
        id = requireText(id, "id");
        issuer = Objects.requireNonNull(issuer, "issuer");
        if (!issuer.isAbsolute()) {
            throw new IllegalArgumentException("issuer must be absolute");
        }
        subject = requireText(subject, "subject");
        authenticationMethod = Objects.requireNonNull(authenticationMethod, "authenticationMethod");
        audiences = copyTextSet(audiences, "audiences");
        scopes = copyTextSet(scopes, "scopes");
        roles = copyTextSet(roles, "roles");
    }

    static Set<String> copyTextSet(Set<String> values, String name) {
        values = Set.copyOf(Objects.requireNonNull(values, name));
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(name + " must not contain blank values");
        }
        return values;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
