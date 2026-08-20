package dev.mrk.aegis;

import java.util.Objects;
import java.util.Optional;

/** A stable, credential-safe authorization result suitable for callers and audit records. */
public record AuthorizationDecision(boolean allowed, DenialReason denialReason) {
    public AuthorizationDecision {
        if (allowed && denialReason != null) {
            throw new IllegalArgumentException("an allowed decision must not have a denial reason");
        }
        if (!allowed) {
            denialReason = Objects.requireNonNull(denialReason, "denialReason");
        }
    }

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(true, null);
    }

    public static AuthorizationDecision deny(DenialReason reason) {
        return new AuthorizationDecision(false, reason);
    }

    public Optional<DenialReason> denialReasonOptional() {
        return Optional.ofNullable(denialReason);
    }
}
