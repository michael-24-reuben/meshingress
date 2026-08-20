package dev.mrk.aegis;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A provider-neutral external identity after a host has verified its evidence.
 * It intentionally carries no token, password, refresh secret, or raw claims.
 */
public record VerifiedIdentity(
        String identityId,
        URI issuer,
        String subject,
        AuthenticationMethod authenticationMethod,
        Set<String> audiences,
        Set<String> scopes,
        Set<String> roles,
        Instant verifiedAt,
        String displayName,
        Map<String, String> displayAttributes
) {
    public VerifiedIdentity {
        identityId = requireText(identityId, "identityId");
        issuer = Objects.requireNonNull(issuer, "issuer");
        if (!issuer.isAbsolute()) throw new IllegalArgumentException("issuer must be absolute");
        subject = requireText(subject, "subject");
        authenticationMethod = Objects.requireNonNull(authenticationMethod, "authenticationMethod");
        audiences = AuthenticatedIdentity.copyTextSet(audiences, "audiences");
        scopes = AuthenticatedIdentity.copyTextSet(scopes, "scopes");
        roles = AuthenticatedIdentity.copyTextSet(roles, "roles");
        verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt");
        if (displayName != null && displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank when provided");
        displayAttributes = AuthProfileMetadata.copyTextMap(displayAttributes, "displayAttributes");
    }

    public AuthenticatedIdentity asProfileIdentity() {
        return new AuthenticatedIdentity(identityId, issuer, subject, authenticationMethod, audiences, scopes, roles, verifiedAt);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
