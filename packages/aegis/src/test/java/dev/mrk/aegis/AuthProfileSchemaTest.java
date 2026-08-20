package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthProfileSchemaTest {

    @Test
    void packagesTheCurrentVersionedProfileSchema() throws IOException {
        try (var input = AuthProfileSchemaTest.class.getResourceAsStream("/META-INF/aegis/auth-profile.schema.json")) {
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(schema.contains("https://schemas.mrk.dev/aegis/auth-profile/v1"));
            assertTrue(schema.contains("\"const\": 1"));
            assertTrue(schema.contains("\"SECRET_REFERENCE\""));
        }
    }
}
