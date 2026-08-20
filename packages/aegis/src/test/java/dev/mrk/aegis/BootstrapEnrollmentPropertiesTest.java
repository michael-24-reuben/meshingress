package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapEnrollmentPropertiesTest {

    @Test
    void loadsSafeModuleDefaultsFromApplicationProperties() {
        BootstrapEnrollmentProperties properties = BootstrapEnrollmentProperties.loadDefault();

        assertFalse(properties.enabled());
        assertEquals("bootstrap-enroll-admin", properties.action());
        assertEquals(Duration.ofMinutes(15), properties.credentialTtl());
        assertEquals(3, properties.maxAttempts());
        assertEquals("admin", properties.administratorRole());
    }

    @Test
    void parsesHostControlledValues() throws Exception {
        String source = """
                aegis.bootstrap.enabled=true
                aegis.bootstrap.action=first-admin
                aegis.bootstrap.credential-ttl=PT2H
                aegis.bootstrap.max-attempts=7
                aegis.bootstrap.administrator-role=operator
                """;

        BootstrapEnrollmentProperties properties = BootstrapEnrollmentProperties.load(
                new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))
        );

        assertTrue(properties.enabled());
        assertEquals("first-admin", properties.action());
        assertEquals(Duration.ofHours(2), properties.credentialTtl());
        assertEquals(7, properties.maxAttempts());
        assertEquals("operator", properties.administratorRole());
    }

    @Test
    void rejectsInvalidDurationAndAttemptLimit() {
        String invalidDuration = """
                aegis.bootstrap.enabled=true
                aegis.bootstrap.action=bootstrap-enroll-admin
                aegis.bootstrap.credential-ttl=tomorrow
                aegis.bootstrap.max-attempts=3
                aegis.bootstrap.administrator-role=admin
                """;
        String invalidAttempts = invalidDuration.replace("tomorrow", "PT15M").replace("=3", "=0");
        String invalidEnabled = invalidDuration.replace("tomorrow", "PT15M").replace("=true", "=enabled");

        assertThrows(IllegalArgumentException.class, () -> BootstrapEnrollmentProperties.load(
                new ByteArrayInputStream(invalidDuration.getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> BootstrapEnrollmentProperties.load(
                new ByteArrayInputStream(invalidAttempts.getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> BootstrapEnrollmentProperties.load(
                new ByteArrayInputStream(invalidEnabled.getBytes(StandardCharsets.UTF_8))));
    }
}
