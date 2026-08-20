package dev.mrk.meshingress.security;

import dev.mrk.aegis.AuthProfile;
import dev.mrk.aegis.AuthProfileMetadata;
import dev.mrk.aegis.AuthenticatedIdentity;
import dev.mrk.aegis.AuthenticationMethod;
import dev.mrk.aegis.ProfileStatus;
import dev.mrk.meshingress.security.management.JdbcProfileStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AegisProfileIdentityResolverTest {
    private static final String GOOGLE = "https://accounts.google.com";

    @Test
    void grantsLocalProfileAuthorityOnlyForOneActiveVerifiedIdentity() {
        JdbcProfileStore store = store();
        UUID activeId = UUID.randomUUID();
        store.create(profile(activeId, ProfileStatus.ACTIVE, "google-jbeas", "subject-1", Set.of("admin"), Set.of("security.manage")));
        store.create(profile(UUID.randomUUID(), ProfileStatus.DISABLED, "google-disabled", "subject-2", Set.of("admin"), Set.of("security.manage")));
        AegisProfileIdentityResolver resolver = new AegisProfileIdentityResolver(store);

        var resolved = resolver.resolve(GOOGLE, "subject-1");

        assertTrue(resolved.isPresent());
        assertEquals(activeId.toString(), resolved.orElseThrow().profileId());
        assertEquals("gmail-personal", resolved.orElseThrow().tenantId());
        assertEquals(Set.of("admin"), resolved.orElseThrow().roles());
        assertEquals(Set.of("security.manage"), resolved.orElseThrow().grants());
        assertFalse(resolver.resolve(GOOGLE, "subject-2").isPresent());
    }

    @Test
    void rejectsDuplicateIssuerSubjectBindings() {
        JdbcProfileStore store = store();
        store.create(profile(UUID.randomUUID(), ProfileStatus.ACTIVE, "google-first", "subject-duplicate", Set.of(), Set.of()));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> store.create(profile(UUID.randomUUID(), ProfileStatus.ACTIVE, "google-second", "subject-duplicate", Set.of(), Set.of())));

        assertEquals("external identity is already bound to another profile", failure.getMessage());
    }

    private static JdbcProfileStore store() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:profile-resolution-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        return new JdbcProfileStore(new JdbcTemplate(dataSource), new ObjectMapper());
    }

    private static AuthProfile profile(UUID profileId, ProfileStatus status, String identityId, String subject, Set<String> roles, Set<String> scopes) {
        Instant now = Instant.parse("2026-08-15T15:00:00Z");
        AuthenticatedIdentity identity = new AuthenticatedIdentity(identityId, URI.create(GOOGLE), subject, AuthenticationMethod.OIDC,
                Set.of("google-client"), scopes, roles, now);
        return new AuthProfile(1, profileId, "gmail-personal", status,
                new AuthProfileMetadata("Google profile", now, now, Map.of("provider", "google")), List.of(identity), List.of());
    }
}
