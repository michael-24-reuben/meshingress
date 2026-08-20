package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthProfileTest {

    @Test
    void acceptsCredentialSafeDelegatedOauthProfile() {
        Instant now = Instant.parse("2026-08-14T12:00:00Z");
        AuthenticatedIdentity identity = identity("user-1");
        CredentialBinding binding = new CredentialBinding(
                "github", "user-1", "github-api", CredentialStrategy.DELEGATED_OAUTH,
                new SecretReference("vault", "aegis/profiles/123/github", "3"),
                Set.of("repo:read"), CredentialBindingState.ACTIVE, now.plusSeconds(3_600)
        );

        AuthProfile profile = new AuthProfile(
                1, UUID.fromString("00000000-0000-0000-0000-000000000123"), ProfileStatus.ACTIVE,
                new AuthProfileMetadata("Jane", now, now, Map.of("tenant", "acme")),
                List.of(identity), List.of(binding)
        );

        assertEquals("vault", profile.credentialBindings().getFirst().secretReference().provider());
    }

    @Test
    void rejectsBindingForUnknownIdentity() {
        Instant now = Instant.parse("2026-08-14T12:00:00Z");
        CredentialBinding binding = new CredentialBinding(
                "github", "unknown", "github-api", CredentialStrategy.NONE,
                null, Set.of(), CredentialBindingState.ACTIVE, null
        );

        assertThrows(IllegalArgumentException.class, () -> new AuthProfile(
                1, UUID.randomUUID(), ProfileStatus.ACTIVE,
                new AuthProfileMetadata(null, now, now, Map.of()),
                List.of(identity("user-1")), List.of(binding)
        ));
    }

    @Test
    void requiresSecretReferenceForDelegatedOauth() {
        assertThrows(IllegalArgumentException.class, () -> new CredentialBinding(
                "github", "user-1", "github-api", CredentialStrategy.DELEGATED_OAUTH,
                null, Set.of(), CredentialBindingState.ACTIVE, null
        ));
    }

    private static AuthenticatedIdentity identity(String id) {
        return new AuthenticatedIdentity(
                id, URI.create("https://issuer.example"), "subject-123", AuthenticationMethod.OIDC,
                Set.of("aegis"), Set.of("tools:read"), Set.of("user"), null
        );
    }
}
