package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void createsReadsListsAndOptimisticallyUpdatesProfiles() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-000000000321");
        ProfileService service = service(profileId);

        ProfileSnapshot created = service.create(draft("primary", List.of(identity("first")), List.of()));
        ProfileSnapshot updated = service.updateMetadata(profileId, 0, "Primary", Map.of("tenant", "acme"));

        assertEquals(0, created.revision());
        assertEquals(ProfileStatus.ACTIVE, created.profile().status());
        assertEquals(1, updated.revision());
        assertEquals("Primary", service.get(profileId).profile().metadata().displayName());
        assertEquals(1, service.list(ProfileQuery.all()).size());
        assertThrows(ProfileRevisionConflictException.class,
                () -> service.updateMetadata(profileId, 0, "Stale", Map.of()));
    }

    @Test
    void statusChangesFilterListsAndRevocationIsTerminal() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-000000000322");
        ProfileService service = service(profileId);
        ProfileSnapshot created = service.create(draft("primary", List.of(identity("first")), List.of(binding("first", "github"))));

        ProfileSnapshot disabled = service.disable(profileId, created.revision());
        assertEquals(ProfileStatus.DISABLED, disabled.profile().status());
        assertEquals(1, service.list(new ProfileQuery(Set.of(ProfileStatus.DISABLED), 10)).size());

        ProfileSnapshot active = service.activate(profileId, disabled.revision());
        ProfileSnapshot revoked = service.revoke(profileId, active.revision());

        assertEquals(ProfileStatus.REVOKED, revoked.profile().status());
        assertEquals(CredentialBindingState.REVOKED, revoked.profile().credentialBindings().getFirst().state());
        assertThrows(IllegalStateException.class, () -> service.activate(profileId, revoked.revision()));
        assertThrows(IllegalStateException.class,
                () -> service.updateMetadata(profileId, revoked.revision(), "No longer mutable", Map.of()));
    }

    @Test
    void preservesIdentityAndBindingReferenceIntegrityAcrossLifecycleOperations() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-000000000323");
        ProfileService service = service(profileId);
        ProfileSnapshot created = service.create(draft("primary", List.of(identity("first")), List.of()));
        ProfileSnapshot linked = service.linkIdentity(profileId, created.revision(), identity("second"));
        ProfileSnapshot bound = service.bindCredential(profileId, linked.revision(), binding("second", "github"));

        assertThrows(IllegalStateException.class, () -> service.unlinkIdentity(profileId, bound.revision(), "second"));

        ProfileSnapshot revoked = service.revokeCredential(profileId, bound.revision(), "github");
        assertEquals(CredentialBindingState.REVOKED, revoked.profile().credentialBindings().getFirst().state());
        ProfileSnapshot unbound = service.unbindCredential(profileId, revoked.revision(), "github");
        ProfileSnapshot unlinked = service.unlinkIdentity(profileId, unbound.revision(), "second");

        assertEquals(List.of("first"), unlinked.profile().identities().stream().map(AuthenticatedIdentity::id).toList());
        assertTrue(unlinked.profile().credentialBindings().isEmpty());
    }

    @Test
    void replacementKeepsBindingIdsStableAndProfileValuesDoNotContainCredentialEvidence() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-000000000324");
        ProfileService service = service(profileId);
        ProfileSnapshot created = service.create(draft("primary", List.of(identity("first")), List.of(binding("first", "github"))));

        CredentialBinding replacement = new CredentialBinding(
                "github", "first", "github-api", CredentialStrategy.DELEGATED_OAUTH,
                new SecretReference("vault", "profiles/324/github", "2"), Set.of("repo:write"),
                CredentialBindingState.ACTIVE, NOW.plusSeconds(3_600)
        );
        ProfileSnapshot replaced = service.replaceCredential(profileId, created.revision(), "github", replacement);

        assertEquals("github", replaced.profile().credentialBindings().getFirst().id());
        assertEquals(Set.of("repo:write"), replaced.profile().credentialBindings().getFirst().grantedScopes());
        assertFalse(replaced.toString().contains("raw-token"));
        assertThrows(IllegalArgumentException.class,
                () -> service.replaceCredential(profileId, replaced.revision(), "github",
                        new CredentialBinding("other", "first", "github-api", CredentialStrategy.NONE,
                                null, Set.of(), CredentialBindingState.ACTIVE, null)));
    }

    private static ProfileService service(UUID profileId) {
        return new ProfileService(new InMemoryProfileStore(), CLOCK, () -> profileId);
    }

    private static ProfileDraft draft(String displayName, List<AuthenticatedIdentity> identities, List<CredentialBinding> bindings) {
        return new ProfileDraft(displayName, Map.of("environment", "test"), identities, bindings);
    }

    private static AuthenticatedIdentity identity(String id) {
        return new AuthenticatedIdentity(
                id, URI.create("https://issuer.example"), "subject-" + id, AuthenticationMethod.OIDC,
                Set.of("aegis"), Set.of("tools:read"), Set.of("user"), NOW
        );
    }

    private static CredentialBinding binding(String identityId, String bindingId) {
        return new CredentialBinding(
                bindingId, identityId, "github-api", CredentialStrategy.DELEGATED_OAUTH,
                new SecretReference("vault", "profiles/test/" + bindingId, "1"), Set.of("repo:read"),
                CredentialBindingState.ACTIVE, NOW.plusSeconds(3_600)
        );
    }
}
