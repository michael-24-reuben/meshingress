package dev.mrk.aegis;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Basic reusable policy that denies expired principals and missing grants. */
public final class GrantAuthorizationPolicy implements AuthorizationPolicy {

    private final Clock clock;

    public GrantAuthorizationPolicy() {
        this(Clock.systemUTC());
    }

    public GrantAuthorizationPolicy(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public AuthorizationDecision evaluate(AuthorizationRequest request) {
        Objects.requireNonNull(request, "request");
        if (!request.principal().expiresAt().isAfter(Instant.now(clock))) {
            return AuthorizationDecision.deny(DenialReason.EXPIRED_CREDENTIAL);
        }
        if (!request.principal().grants().containsAll(request.requiredGrants())) {
            return AuthorizationDecision.deny(DenialReason.MISSING_REQUIRED_GRANT);
        }
        return AuthorizationDecision.allow();
    }
}
