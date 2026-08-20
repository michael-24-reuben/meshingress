package dev.mrk.aegis;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Credential-safe outcome of a bootstrap enrollment attempt. */
public record BootstrapEnrollmentResult(boolean enrolled, UUID profileId, DenialReason denialReason) {
    public BootstrapEnrollmentResult {
        if (enrolled) {
            profileId = Objects.requireNonNull(profileId, "profileId");
            if (denialReason != null) {
                throw new IllegalArgumentException("an enrolled result must not have a denial reason");
            }
        } else {
            if (profileId != null) {
                throw new IllegalArgumentException("a denied result must not include a profile id");
            }
            denialReason = Objects.requireNonNull(denialReason, "denialReason");
        }
    }

    public static BootstrapEnrollmentResult enrolled(UUID profileId) {
        return new BootstrapEnrollmentResult(true, profileId, null);
    }

    public static BootstrapEnrollmentResult denied(DenialReason reason) {
        return new BootstrapEnrollmentResult(false, null, reason);
    }

    public Optional<DenialReason> denialReasonOptional() {
        return Optional.ofNullable(denialReason);
    }
}
