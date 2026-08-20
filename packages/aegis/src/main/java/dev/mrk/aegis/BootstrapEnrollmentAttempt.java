package dev.mrk.aegis;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Credential-safe data that the store must process as one atomic enrollment transaction. */
public record BootstrapEnrollmentAttempt(UUID issuanceId, AuthProfile administratorProfile, Instant now) {
    public BootstrapEnrollmentAttempt {
        issuanceId = Objects.requireNonNull(issuanceId, "issuanceId");
        administratorProfile = Objects.requireNonNull(administratorProfile, "administratorProfile");
        now = Objects.requireNonNull(now, "now");
    }
}
