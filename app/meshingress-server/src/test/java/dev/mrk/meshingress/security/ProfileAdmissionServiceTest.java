package dev.mrk.meshingress.security;

import dev.mrk.aegis.AuthenticationMethod;
import dev.mrk.aegis.ProfileStatus;
import dev.mrk.aegis.VerifiedIdentity;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.security.management.JdbcProfileStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileAdmissionServiceTest {
    @Test
    void closedAdmissionDoesNotCreateAProfile() {
        ProfileAdmissionService service = service(MeshingressProperties.Security.AdmissionMode.CLOSED);
        var result = service.admit(identity("subject-a"));
        assertFalse(result.created());
        assertEquals("ADMISSION_CLOSED", result.reason());
    }

    @Test
    void selfServiceCreatesOnePendingUnprivilegedProfilePerVerifiedIdentity() {
        ProfileAdmissionService service = service(MeshingressProperties.Security.AdmissionMode.SELF_SERVICE_UNPRIVILEGED);
        var first = service.admit(identity("subject-a"));
        var second = service.admit(identity("subject-a"));

        assertTrue(first.created());
        assertEquals(ProfileStatus.PENDING_REVIEW, first.profile().profile().status());
        assertFalse(second.created());
        assertEquals(first.profile().profile().profileId(), second.profile().profile().profileId());
        assertTrue(first.profile().profile().identities().getFirst().roles().isEmpty());
    }

    private ProfileAdmissionService service(MeshingressProperties.Security.AdmissionMode mode) {
        JdbcProfileStore store = new JdbcProfileStore(new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:admission-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "")), new ObjectMapper());
        MeshingressProperties.Security security = new MeshingressProperties.Security(true, "dev", false, false, "", false, true, true, true, true,
                new MeshingressProperties.Security.Oidc(false, "", "", "tenant_id", "profile_id", "roles", "scope"),
                new MeshingressProperties.Security.Admission(mode, "local", MeshingressProperties.Security.InitialProfileStatus.PENDING_REVIEW),
                null);
        return new ProfileAdmissionService(store, new MeshingressProperties(null, null, null, null, null, security, null, null, null, null, null));
    }

    private VerifiedIdentity identity(String subject) {
        return new VerifiedIdentity("identity-" + subject, URI.create("https://accounts.google.com"), subject, AuthenticationMethod.OIDC,
                Set.of("client"), Set.of(), Set.of(), Instant.parse("2026-08-15T20:00:00Z"), "A Test User", Map.of("email", "a@example.test"));
    }
}
