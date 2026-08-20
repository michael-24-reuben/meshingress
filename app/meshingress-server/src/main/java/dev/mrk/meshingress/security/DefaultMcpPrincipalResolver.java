package dev.mrk.meshingress.security;

import dev.mrk.meshingress.api.McpAuthenticationMethod;
import dev.mrk.meshingress.api.McpPrincipal;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts a verified Spring Security authentication. The configured development
 * token is deliberately limited to dev mode and exists only for migration
 * tests; it never accepts caller-supplied role headers.
 */
@Component
public class DefaultMcpPrincipalResolver implements McpPrincipalResolver {
    private final MeshingressProperties properties;
    private final AegisProfileIdentityResolver profileIdentities;
    private final VerifiedJwtIdentityFactory verifiedIdentities;
    private final ProfileAdmissionService admissions;

    public DefaultMcpPrincipalResolver(MeshingressProperties properties, AegisProfileIdentityResolver profileIdentities,
                                       VerifiedJwtIdentityFactory verifiedIdentities, ProfileAdmissionService admissions) {
        this.properties = properties;
        this.profileIdentities = profileIdentities;
        this.verifiedIdentities = verifiedIdentities;
        this.admissions = admissions;
    }

    @Override
    public McpPrincipal resolve(McpTransportEvidence evidence) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                Jwt jwt = jwtAuthentication.getToken();
                var verified = verifiedIdentities.from(jwt);
                String issuer = verified.issuer().toString();
                var local = profileIdentities.resolve(verified);
                if (local.isEmpty()) {
                    admissions.admit(verified);
                    local = profileIdentities.resolve(verified);
                }
                if (local.isPresent()) {
                    AegisProfileIdentityResolver.ResolvedIdentity identity = local.get();
                    return new McpPrincipal(jwt.getSubject(), issuer, McpAuthenticationMethod.OIDC_JWT, jwt.getExpiresAt(), identity.roles(), identity.grants(),
                            identity.profileId(), identity.tenantId());
                }
                // An authenticated external identity without a local Aegis binding has no local
                // Meshingress roles, grants, tenant, profile, or quota ownership.
                return new McpPrincipal(jwt.getSubject(), issuer, McpAuthenticationMethod.OIDC_JWT, jwt.getExpiresAt(), Set.of(), Set.of(), "", "");
            }
            Set<String> roles = new LinkedHashSet<>();
            Set<String> grants = new LinkedHashSet<>();
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String value = authority.getAuthority();
                if (value == null || value.isBlank()) continue;
                if (value.startsWith("ROLE_")) roles.add(value.substring("ROLE_".length())); else grants.add(value);
            }
            return new McpPrincipal(authentication.getName(), "spring-security", McpAuthenticationMethod.SPRING_SECURITY, null, roles, grants, "", "");
        }
        if (developmentTokenMatches(evidence.authorization())) {
            return new McpPrincipal("development-admin", "meshingress-dev", McpAuthenticationMethod.DEVELOPMENT_TOKEN,
                    null, Set.of("admin"), Set.of(), "", "");
        }
        return McpPrincipal.anonymous();
    }

    private static String textClaim(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        return value instanceof String text ? text : "";
    }

    private static Set<String> claimValues(Object value) {
        if (value instanceof String text) return Set.of(text.split("\\\\s+"));
        if (value instanceof java.util.Collection<?> values) {
            Set<String> result = new LinkedHashSet<>();
            for (Object candidate : values) if (candidate instanceof String text && !text.isBlank()) result.add(text);
            return result;
        }
        return Set.of();
    }

    private boolean developmentTokenMatches(String authorization) {
        MeshingressProperties.Security security = properties.security();
        return "dev".equalsIgnoreCase(security.mode())
                && security.legacyDevelopmentTokenEnabled()
                && security.developmentAdminToken() != null
                && !security.developmentAdminToken().isBlank()
                && ("Bearer " + security.developmentAdminToken()).equals(authorization == null ? "" : authorization.strip());
    }
}
