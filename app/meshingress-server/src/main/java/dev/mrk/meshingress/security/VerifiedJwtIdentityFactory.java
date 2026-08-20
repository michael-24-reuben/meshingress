package dev.mrk.meshingress.security;

import dev.mrk.aegis.AuthenticationMethod;
import dev.mrk.aegis.VerifiedIdentity;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Converts an already verified Spring JWT into the provider-neutral Aegis identity contract. */
@Component
public final class VerifiedJwtIdentityFactory {
    private final MeshingressProperties properties;

    public VerifiedJwtIdentityFactory(MeshingressProperties properties) { this.properties = properties; }

    public VerifiedIdentity from(Jwt jwt) {
        URI issuer = jwt.getIssuer() == null ? null : URI.create(jwt.getIssuer().toString());
        if (issuer == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalArgumentException("verified JWT must include issuer and subject");
        }
        MeshingressProperties.Security.Oidc oidc = properties.security().oidc();
        Map<String, String> attributes = new LinkedHashMap<>();
        text(jwt, "email").ifPresent(value -> attributes.put("email", value));
        text(jwt, "picture").ifPresent(value -> attributes.put("picture", value));
        String displayName = text(jwt, "name").orElse(null);
        return new VerifiedIdentity(
                stableId(issuer.toString(), jwt.getSubject()), issuer, jwt.getSubject(), AuthenticationMethod.OIDC,
                Set.copyOf(jwt.getAudience()), claimValues(jwt.getClaims().get(oidc.grantsClaim())),
                claimValues(jwt.getClaims().get(oidc.rolesClaim())), jwt.getIssuedAt() == null ? Instant.now() : jwt.getIssuedAt(),
                displayName, attributes);
    }

    private static java.util.Optional<String> text(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        return value instanceof String text && !text.isBlank() ? java.util.Optional.of(text.trim()) : java.util.Optional.empty();
    }

    private static Set<String> claimValues(Object value) {
        Set<String> values = new LinkedHashSet<>();
        if (value instanceof String text) for (String candidate : text.split("\\s+")) if (!candidate.isBlank()) values.add(candidate);
        if (value instanceof java.util.Collection<?> collection) for (Object candidate : collection) if (candidate instanceof String text && !text.isBlank()) values.add(text.trim());
        return Set.copyOf(values);
    }

    private static String stableId(String issuer, String subject) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((issuer + "\u0000" + subject).getBytes(StandardCharsets.UTF_8));
            return "external-" + java.util.HexFormat.of().formatHex(digest, 0, 16);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
