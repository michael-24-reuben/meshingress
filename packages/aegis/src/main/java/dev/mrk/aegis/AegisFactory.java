package dev.mrk.aegis;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * The credential boundary for an application. It validates supplied evidence
 * and returns a capability only when the required grants are present.
 */
public final class AegisFactory {

    private final CredentialVerifier credentialVerifier;
    private final Clock clock;

    public AegisFactory(CredentialVerifier credentialVerifier) {
        this(credentialVerifier, Clock.systemUTC());
    }

    public AegisFactory(CredentialVerifier credentialVerifier, Clock clock) {
        this.credentialVerifier = Objects.requireNonNull(credentialVerifier, "credentialVerifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Acquires the requested capability without exposing the supplied credential.
     */
    public <C extends Capability> CapabilityResult<C> acquire(
            Credential credential,
            CapabilityDefinition<C> definition
    ) {
        Objects.requireNonNull(credential, "credential");
        Objects.requireNonNull(definition, "definition");

        CredentialVerification verification = credentialVerifier.verify(credential);
        if (verification instanceof CredentialVerification.Rejected rejected) {
            return CapabilityResult.denied(rejected.reason());
        }

        VerifiedCredential verified = ((CredentialVerification.Accepted) verification).credential();
        if (!verified.expiresAt().isAfter(Instant.now(clock))) {
            return CapabilityResult.denied(DenialReason.EXPIRED_CREDENTIAL);
        }
        if (!verified.grants().containsAll(definition.requiredGrants())) {
            return CapabilityResult.denied(DenialReason.MISSING_REQUIRED_GRANT);
        }

        return CapabilityResult.granted(definition.provider().create(verified));
    }
}
