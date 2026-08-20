package dev.mrk.aegis;

import java.util.Objects;
import java.util.UUID;

/** Result of validating host-defined evidence for a particular bootstrap issuance. */
public sealed interface BootstrapCredentialVerification {
    record Accepted(UUID issuanceId) implements BootstrapCredentialVerification {
        public Accepted {
            issuanceId = Objects.requireNonNull(issuanceId, "issuanceId");
        }
    }

    record Rejected(DenialReason reason) implements BootstrapCredentialVerification {
        public Rejected {
            reason = Objects.requireNonNull(reason, "reason");
        }
    }
}
