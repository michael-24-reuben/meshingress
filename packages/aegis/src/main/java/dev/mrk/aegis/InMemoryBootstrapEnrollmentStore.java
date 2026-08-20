package dev.mrk.aegis;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe reference store for tests and local demonstrations only. Hosts
 * must provide a durable transactional implementation for production use.
 */
public final class InMemoryBootstrapEnrollmentStore implements BootstrapEnrollmentStore {
    private final Map<UUID, BootstrapIssuance> issuances = new HashMap<>();
    private final Map<UUID, AuthProfile> profiles = new HashMap<>();

    @Override
    public synchronized void issue(BootstrapIssuance issuance) {
        Objects.requireNonNull(issuance, "issuance");
        if (issuances.putIfAbsent(issuance.id(), issuance) != null) {
            throw new IllegalArgumentException("bootstrap issuance already exists");
        }
    }

    @Override
    public synchronized void revoke(UUID issuanceId, Instant now) {
        BootstrapIssuance issuance = requiredIssuance(issuanceId);
        issuances.put(issuanceId, issuance.transitionTo(BootstrapIssuanceState.REVOKED, Objects.requireNonNull(now, "now")));
    }

    @Override
    public synchronized void rotate(UUID priorIssuanceId, BootstrapIssuance replacement, Instant now) {
        Objects.requireNonNull(replacement, "replacement");
        if (replacement.id().equals(priorIssuanceId)) {
            throw new IllegalArgumentException("replacement issuance must use a new id");
        }
        revoke(priorIssuanceId, now);
        issue(replacement);
    }

    @Override
    public synchronized BootstrapEnrollmentResult enroll(
            BootstrapEnrollmentAttempt attempt,
            AdministratorPredicate administratorPredicate
    ) {
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(administratorPredicate, "administratorPredicate");
        BootstrapIssuance issuance = issuances.get(attempt.issuanceId());
        if (issuance == null) {
            return BootstrapEnrollmentResult.denied(DenialReason.BOOTSTRAP_INVALID_CREDENTIAL);
        }
        DenialReason lifecycleDenial = lifecycleDenial(issuance, attempt.now());
        if (lifecycleDenial != null) {
            return BootstrapEnrollmentResult.denied(lifecycleDenial);
        }
        if (profiles.values().stream().anyMatch(administratorPredicate::isAdministrator)) {
            return BootstrapEnrollmentResult.denied(DenialReason.BOOTSTRAP_ADMINISTRATOR_EXISTS);
        }

        BootstrapIssuance attempted = issuance.attempted();
        issuances.put(issuance.id(), attempted);
        if (!administratorPredicate.isAdministrator(attempt.administratorProfile())) {
            exhaustIfLimitReached(attempted, attempt.now());
            return BootstrapEnrollmentResult.denied(DenialReason.BOOTSTRAP_PROFILE_NOT_ADMINISTRATOR);
        }
        if (profiles.containsKey(attempt.administratorProfile().profileId())) {
            exhaustIfLimitReached(attempted, attempt.now());
            return BootstrapEnrollmentResult.denied(DenialReason.BOOTSTRAP_PROFILE_ALREADY_EXISTS);
        }

        profiles.put(attempt.administratorProfile().profileId(), attempt.administratorProfile());
        issuances.put(issuance.id(), attempted.transitionTo(BootstrapIssuanceState.CONSUMED, attempt.now()));
        return BootstrapEnrollmentResult.enrolled(attempt.administratorProfile().profileId());
    }

    @Override
    public synchronized Optional<BootstrapIssuance> findIssuance(UUID issuanceId) {
        return Optional.ofNullable(issuances.get(Objects.requireNonNull(issuanceId, "issuanceId")));
    }

    private BootstrapIssuance requiredIssuance(UUID issuanceId) {
        BootstrapIssuance issuance = issuances.get(Objects.requireNonNull(issuanceId, "issuanceId"));
        if (issuance == null) {
            throw new IllegalArgumentException("unknown bootstrap issuance");
        }
        return issuance;
    }

    private static DenialReason lifecycleDenial(BootstrapIssuance issuance, Instant now) {
        if (!issuance.expiresAt().isAfter(now)) {
            return DenialReason.BOOTSTRAP_CREDENTIAL_EXPIRED;
        }
        return switch (issuance.state()) {
            case ACTIVE -> issuance.attempts() >= issuance.maxAttempts()
                    ? DenialReason.BOOTSTRAP_ATTEMPTS_EXHAUSTED : null;
            case CONSUMED -> DenialReason.BOOTSTRAP_CREDENTIAL_CONSUMED;
            case REVOKED -> DenialReason.BOOTSTRAP_CREDENTIAL_REVOKED;
            case EXHAUSTED -> DenialReason.BOOTSTRAP_ATTEMPTS_EXHAUSTED;
        };
    }

    private void exhaustIfLimitReached(BootstrapIssuance issuance, Instant now) {
        if (issuance.attempts() >= issuance.maxAttempts()) {
            issuances.put(issuance.id(), issuance.transitionTo(BootstrapIssuanceState.EXHAUSTED, now));
        }
    }
}
