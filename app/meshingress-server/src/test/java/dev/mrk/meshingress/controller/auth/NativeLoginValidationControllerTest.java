package dev.mrk.meshingress.controller.auth;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeLoginValidationControllerTest {

    @Test
    void developmentValidationAcknowledgesSubmissionWithoutReturningThePassword() {
        NativeLoginValidationController controller = new NativeLoginValidationController(properties("dev"));

        var response = controller.validate(
                new NativeLoginValidationController.NativeLoginValidationRequest("ada@example.test", "never-return-this"),
                "request-42"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().accepted());
        assertEquals("request-42", response.getBody().requestId());
        assertFalse(response.getBody().toString().contains("never-return-this"));
    }

    @Test
    void productionDoesNotExposeTheDevelopmentValidationRoute() {
        NativeLoginValidationController controller = new NativeLoginValidationController(properties("production"));

        var response = controller.validate(
                new NativeLoginValidationController.NativeLoginValidationRequest("ada@example.test", "password"),
                "request-42"
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void browserConfigurationExposesOnlyThePublicGoogleClientId() {
        MeshingressProperties.Security.Oidc oidc = new MeshingressProperties.Security.Oidc(
                true, "https://accounts.google.com", "browser-client-id", "tenant_id", "profile_id", "roles", "scope"
        );
        MeshingressProperties.Security security = new MeshingressProperties.Security(
                true, "dev", false, false, "", false, true, true, true, true, oidc
        );
        AuthenticationRuntimeController controller = new AuthenticationRuntimeController(
                new MeshingressProperties(null, null, null, null, null, security, null, null, null, null, null)
        );

        assertEquals(java.util.Map.of("googleClientId", "browser-client-id"), controller.configuration());
    }

    private static MeshingressProperties properties(String mode) {
        MeshingressProperties.Security security = new MeshingressProperties.Security(
                true, mode, false, false, "", false, true, true, true, true
        );
        return new MeshingressProperties(null, null, null, null, null, security, null, null, null, null, null);
    }
}
