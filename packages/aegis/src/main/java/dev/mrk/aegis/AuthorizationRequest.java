package dev.mrk.aegis;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generic, credential-safe input to authorization policy. Attributes must be
 * allowlisted policy inputs, never raw request headers or credential values.
 */
public record AuthorizationRequest(
        VerifiedCredential principal,
        String action,
        String resource,
        Set<String> requiredGrants,
        Map<String, String> attributes
) {
    public AuthorizationRequest {
        principal = Objects.requireNonNull(principal, "principal");
        action = requireText(action, "action");
        resource = requireText(resource, "resource");
        requiredGrants = AuthenticatedIdentity.copyTextSet(requiredGrants, "requiredGrants");
        attributes = AuthProfileMetadata.copyTextMap(attributes, "attributes");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
