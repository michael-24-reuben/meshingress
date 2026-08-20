package dev.mrk.aegis;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Input for a newly active, credential-safe profile. */
public record ProfileDraft(
        String tenantId,
        String displayName,
        Map<String, String> labels,
        List<AuthenticatedIdentity> identities,
        List<CredentialBinding> credentialBindings
) {
    public ProfileDraft {
        tenantId = tenantId == null ? "" : tenantId.trim();
        if (displayName != null && displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank when provided");
        }
        labels = Map.copyOf(Objects.requireNonNull(labels, "labels"));
        identities = List.copyOf(Objects.requireNonNull(identities, "identities"));
        credentialBindings = List.copyOf(Objects.requireNonNull(credentialBindings, "credentialBindings"));
    }

    /** Compatibility constructor for a host that has no tenant concept. */
    public ProfileDraft(
            String displayName,
            Map<String, String> labels,
            List<AuthenticatedIdentity> identities,
            List<CredentialBinding> credentialBindings
    ) {
        this("", displayName, labels, identities, credentialBindings);
    }
}
