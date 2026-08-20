package dev.mrk.meshingress.controller.auth;

import dev.mrk.meshingress.security.ProfileAdmissionService;
import dev.mrk.meshingress.security.VerifiedJwtIdentityFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** HTTPS admission/introspection endpoint. Its input is only Spring's verified JWT principal. */
@RestController
@RequestMapping("/api/v1/auth/profile")
public final class ProfileAdmissionController {
    private final VerifiedJwtIdentityFactory identities;
    private final ProfileAdmissionService admissions;
    private final ObjectMapper objectMapper;

    public ProfileAdmissionController(VerifiedJwtIdentityFactory identities, ProfileAdmissionService admissions, ObjectMapper objectMapper) {
        this.identities = identities; this.admissions = admissions; this.objectMapper = objectMapper;
    }

    @PostMapping("/admit")
    public ObjectNode admit(Authentication authentication) {
        var result = admissions.admit(identity(authentication));
        ObjectNode response = objectMapper.createObjectNode();
        response.put("admitted", result.profile() != null);
        response.put("created", result.created());
        response.put("reason", result.reason());
        if (result.profile() != null) {
            response.put("profileId", result.profile().profile().profileId().toString());
            response.put("status", result.profile().profile().status().name());
            response.put("tenantId", result.profile().profile().tenantId());
        }
        return response;
    }

    @GetMapping("/me")
    public ObjectNode me(Authentication authentication) {
        var result = admissions.admit(identity(authentication));
        ObjectNode response = objectMapper.createObjectNode();
        response.put("admitted", result.profile() != null);
        response.put("reason", result.reason());
        if (result.profile() != null) {
            response.put("profileId", result.profile().profile().profileId().toString());
            response.put("status", result.profile().profile().status().name());
            response.put("tenantId", result.profile().profile().tenantId());
        }
        return response;
    }

    private dev.mrk.aegis.VerifiedIdentity identity(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A verified bearer identity is required.");
        }
        return identities.from(jwt.getToken());
    }
}
