package dev.mrk.aegis;

import java.util.Objects;

/** Result of a credential verifier without exposing an underlying secret. */
public sealed interface CredentialVerification
        permits CredentialVerification.Accepted, CredentialVerification.Rejected {

    record Accepted(VerifiedCredential credential) implements CredentialVerification {
        public Accepted {
            credential = Objects.requireNonNull(credential, "credential");
        }
    }

    record Rejected(DenialReason reason) implements CredentialVerification {
        public Rejected {
            reason = Objects.requireNonNull(reason, "reason");
            if (reason == DenialReason.MISSING_REQUIRED_GRANT) {
                throw new IllegalArgumentException("A credential rejection must use a credential denial reason");
            }
        }
    }
}
