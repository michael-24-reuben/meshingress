package dev.mrk.aegis;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Identity and grants obtained only after a host-provided verifier accepts a credential.
 * It intentionally contains no raw token, secret, or private key material.
 */
public record VerifiedCredential(
        String subject,
        Set<String> grants,
        Instant expiresAt
) {
    public VerifiedCredential {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        grants = Set.copyOf(Objects.requireNonNull(grants, "grants"));
        if (grants.stream().anyMatch(grant -> grant == null || grant.isBlank())) {
            throw new IllegalArgumentException("grants must not contain blank values");
        }
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
