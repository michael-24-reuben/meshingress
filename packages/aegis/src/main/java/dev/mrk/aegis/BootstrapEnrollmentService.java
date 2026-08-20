package dev.mrk.aegis;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates explicit bootstrap issuance and enrollment. It neither creates
 * credentials nor stores raw secret material; those remain host concerns.
 */
public final class BootstrapEnrollmentService {
    private final BootstrapEnrollmentProperties properties;
    private final BootstrapCredentialVerifier credentialVerifier;
    private final BootstrapEnrollmentStore store;
    private final AdministratorPredicate administratorPredicate;
    private final Clock clock;

    public BootstrapEnrollmentService(
            BootstrapEnrollmentProperties properties,
            BootstrapCredentialVerifier credentialVerifier,
            BootstrapEnrollmentStore store
    ) {
        this(properties, credentialVerifier, store,
                new RoleAdministratorPredicate(properties.administratorRole()), Clock.systemUTC());
    }

    public BootstrapEnrollmentService(
            BootstrapEnrollmentProperties properties,
            BootstrapCredentialVerifier credentialVerifier,
            BootstrapEnrollmentStore store,
            AdministratorPredicate administratorPredicate,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.credentialVerifier = Objects.requireNonNull(credentialVerifier, "credentialVerifier");
        this.store = Objects.requireNonNull(store, "store");
        this.administratorPredicate = Objects.requireNonNull(administratorPredicate, "administratorPredicate");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Explicit host-controlled issuance; it is never called automatically by construction or restart. */
    public BootstrapIssuance issue(UUID issuanceId) {
        requireEnabled();
        BootstrapIssuance issuance = BootstrapIssuance.issue(issuanceId, Instant.now(clock), properties);
        store.issue(issuance);
        return issuance;
    }

    /** Explicit host-controlled rotation that revokes the preceding issuance atomically in the store. */
    public BootstrapIssuance rotate(UUID priorIssuanceId, UUID replacementIssuanceId) {
        requireEnabled();
        Instant now = Instant.now(clock);
        BootstrapIssuance replacement = BootstrapIssuance.issue(replacementIssuanceId, now, properties);
        store.rotate(priorIssuanceId, replacement, now);
        return replacement;
    }

    public void revoke(UUID issuanceId) {
        store.revoke(issuanceId, Instant.now(clock));
    }

    public BootstrapEnrollmentResult enroll(Credential credential, AuthProfile administratorProfile) {
        return enroll(properties.action(), credential, administratorProfile);
    }

    /** Only the configured bootstrap action can use bootstrap evidence. */
    public BootstrapEnrollmentResult enroll(String action, Credential credential, AuthProfile administratorProfile) {
        if (!properties.enabled()) {
            return BootstrapEnrollmentResult.denied(DenialReason.BOOTSTRAP_DISABLED);
        }
        if (!properties.action().equals(action)) {
            return BootstrapEnrollmentResult.denied(DenialReason.BOOTSTRAP_INVALID_ACTION);
        }
        BootstrapCredentialVerification verification = credentialVerifier.verify(Objects.requireNonNull(credential, "credential"));
        if (verification instanceof BootstrapCredentialVerification.Rejected rejected) {
            return BootstrapEnrollmentResult.denied(rejected.reason());
        }
        UUID issuanceId = ((BootstrapCredentialVerification.Accepted) verification).issuanceId();
        return store.enroll(new BootstrapEnrollmentAttempt(issuanceId, administratorProfile, Instant.now(clock)), administratorPredicate);
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new IllegalStateException("Aegis bootstrap enrollment is disabled");
        }
    }
}
