package dev.mrk.aegis;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Connects one profile identity to one upstream target using an explicit
 * credential strategy. Secret-backed strategies retain only a reference.
 */
public record CredentialBinding(
        String id,
        String identityId,
        String target,
        CredentialStrategy strategy,
        SecretReference secretReference,
        Set<String> grantedScopes,
        CredentialBindingState state,
        Instant expiresAt
) {
    public CredentialBinding {
        id = requireText(id, "id");
        identityId = requireText(identityId, "identityId");
        target = requireText(target, "target");
        strategy = Objects.requireNonNull(strategy, "strategy");
        if (requiresSecretReference(strategy) && secretReference == null) {
            throw new IllegalArgumentException(strategy + " requires a secretReference");
        }
        if (!requiresSecretReference(strategy) && secretReference != null) {
            throw new IllegalArgumentException(strategy + " must not retain a secretReference");
        }
        grantedScopes = AuthenticatedIdentity.copyTextSet(grantedScopes, "grantedScopes");
        state = Objects.requireNonNull(state, "state");
    }

    private static boolean requiresSecretReference(CredentialStrategy strategy) {
        return strategy == CredentialStrategy.SECRET_REFERENCE
                || strategy == CredentialStrategy.DELEGATED_OAUTH;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
