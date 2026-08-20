package dev.mrk.meshingress.controller.auth;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Exposes only public browser configuration required by the Studio login surface. */
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthenticationRuntimeController {
    private final MeshingressProperties properties;

    public AuthenticationRuntimeController(MeshingressProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public Map<String, String> configuration() {
        MeshingressProperties.Security.Oidc oidc = properties.security().oidc();
        if (!oidc.enabled() || oidc.audience().isBlank()
                || !"https://accounts.google.com".equals(oidc.issuerUri())) {
            return Map.of();
        }
        // OAuth client IDs are public browser identifiers; no secret is exposed.
        return Map.of("googleClientId", oidc.audience());
    }
}
