package dev.mrk.aegis;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Host persistence boundary. {@link #enroll} must atomically enforce the
 * first-administrator check, consume the issuance, and persist the profile.
 */
public interface BootstrapEnrollmentStore {
    void issue(BootstrapIssuance issuance);

    void revoke(UUID issuanceId, Instant now);

    void rotate(UUID priorIssuanceId, BootstrapIssuance replacement, Instant now);

    BootstrapEnrollmentResult enroll(BootstrapEnrollmentAttempt attempt, AdministratorPredicate administratorPredicate);

    Optional<BootstrapIssuance> findIssuance(UUID issuanceId);
}
