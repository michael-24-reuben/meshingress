package dev.mrk.meshingress.security;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshingressOidcPropertiesBindingTest {

    @Test
    void bindsOidcValuesThroughTheCanonicalSecurityConstructor() {
        MeshingressProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
                "meshingress.security.oidc.enabled", "true",
                "meshingress.security.oidc.issuer-uri", "https://accounts.google.com",
                "meshingress.security.oidc.audience", "browser-client-id"
        ))).bind("meshingress", Bindable.of(MeshingressProperties.class))
                .orElseThrow(() -> new AssertionError("Meshingress properties were not bound"));

        MeshingressProperties.Security.Oidc oidc = properties.security().oidc();

        assertTrue(oidc.enabled());
        assertEquals("https://accounts.google.com", oidc.issuerUri());
        assertEquals("browser-client-id", oidc.audience());
    }
}
