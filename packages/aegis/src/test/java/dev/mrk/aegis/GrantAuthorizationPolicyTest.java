package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantAuthorizationPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
    private final GrantAuthorizationPolicy policy = new GrantAuthorizationPolicy(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void allowsAnUnexpiredPrincipalWithAllRequiredGrants() {
        AuthorizationDecision decision = policy.evaluate(request(Set.of("tools:call"), NOW.plusSeconds(60)));

        assertTrue(decision.allowed());
        assertTrue(decision.denialReasonOptional().isEmpty());
    }

    @Test
    void deniesAnExpiredPrincipalBeforeGrantEvaluation() {
        AuthorizationDecision decision = policy.evaluate(request(Set.of("tools:call"), NOW));

        assertEquals(DenialReason.EXPIRED_CREDENTIAL, decision.denialReason());
    }

    @Test
    void deniesAPrincipalMissingTheRequestedGrant() {
        AuthorizationDecision decision = policy.evaluate(request(Set.of("tools:list"), NOW.plusSeconds(60)));

        assertEquals(DenialReason.MISSING_REQUIRED_GRANT, decision.denialReason());
    }

    private static AuthorizationRequest request(Set<String> grants, Instant expiresAt) {
        return new AuthorizationRequest(
                new VerifiedCredential("subject-123", grants, expiresAt),
                "tools/call", "example.tool.function", Set.of("tools:call"), Map.of("tenant", "acme")
        );
    }
}
