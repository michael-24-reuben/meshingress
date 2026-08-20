package dev.mrk.aegis;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Credential-safe bootstrap issuance metadata. It carries an opaque issuance
 * id only; hosts retain a digest or external secret reference separately.
 */
public record BootstrapIssuance(
        UUID id,
        Instant issuedAt,
        Instant expiresAt,
        int maxAttempts,
        int attempts,
        BootstrapIssuanceState state,
        Instant stateChangedAt
) {
    public BootstrapIssuance {
        id = Objects.requireNonNull(id, "id");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        if (maxAttempts < 1 || attempts < 0 || attempts > maxAttempts) {
            throw new IllegalArgumentException("attempts must be between zero and maxAttempts");
        }
        state = Objects.requireNonNull(state, "state");
        stateChangedAt = Objects.requireNonNull(stateChangedAt, "stateChangedAt");
    }

    public static BootstrapIssuance issue(UUID id, Instant now, BootstrapEnrollmentProperties properties) {
        Objects.requireNonNull(properties, "properties");
        return new BootstrapIssuance(id, now, now.plus(properties.credentialTtl()), properties.maxAttempts(), 0,
                BootstrapIssuanceState.ACTIVE, now);
    }

    BootstrapIssuance attempted() {
        return new BootstrapIssuance(id, issuedAt, expiresAt, maxAttempts, attempts + 1, state, stateChangedAt);
    }

    BootstrapIssuance transitionTo(BootstrapIssuanceState next, Instant now) {
        return new BootstrapIssuance(id, issuedAt, expiresAt, maxAttempts, attempts, next, now);
    }
}
