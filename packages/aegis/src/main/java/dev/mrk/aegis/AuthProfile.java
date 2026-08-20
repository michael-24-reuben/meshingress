package dev.mrk.aegis;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Versioned, credential-safe profile for a user or workload identity.
 * Raw credentials are deliberately excluded; use {@link SecretReference}.
 */
public record AuthProfile(
        int schemaVersion,
        UUID profileId,
        String tenantId,
        ProfileStatus status,
        AuthProfileMetadata metadata,
        List<AuthenticatedIdentity> identities,
        List<CredentialBinding> credentialBindings,
        ResourceLimitPolicy limitPolicy
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public AuthProfile {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported auth profile schema version: " + schemaVersion);
        }
        profileId = Objects.requireNonNull(profileId, "profileId");
        tenantId = tenantId == null ? "" : tenantId.trim();
        status = Objects.requireNonNull(status, "status");
        metadata = Objects.requireNonNull(metadata, "metadata");
        identities = List.copyOf(Objects.requireNonNull(identities, "identities"));
        credentialBindings = List.copyOf(Objects.requireNonNull(credentialBindings, "credentialBindings"));
        limitPolicy = Objects.requireNonNullElseGet(limitPolicy, ResourceLimitPolicy::inheritAll);
        if (identities.isEmpty()) {
            throw new IllegalArgumentException("identities must not be empty");
        }

        Set<String> identityIds = uniqueIdentityIds(identities);
        Set<String> bindingIds = new HashSet<>();
        for (CredentialBinding binding : credentialBindings) {
            if (!bindingIds.add(binding.id())) {
                throw new IllegalArgumentException("credentialBindings must have unique ids");
            }
            if (!identityIds.contains(binding.identityId())) {
                throw new IllegalArgumentException("credential binding references an unknown identity: " + binding.identityId());
            }
        }
    }

    /** Compatibility constructor for host integrations that do not use tenancy. */
    public AuthProfile(
            int schemaVersion,
            UUID profileId,
            ProfileStatus status,
            AuthProfileMetadata metadata,
            List<AuthenticatedIdentity> identities,
            List<CredentialBinding> credentialBindings
    ) {
        this(schemaVersion, profileId, "", status, metadata, identities, credentialBindings, ResourceLimitPolicy.inheritAll());
    }

    /** Compatibility constructor for schema-v1 hosts before profile limits existed. */
    public AuthProfile(int schemaVersion, UUID profileId, String tenantId, ProfileStatus status,
                       AuthProfileMetadata metadata, List<AuthenticatedIdentity> identities,
                       List<CredentialBinding> credentialBindings) {
        this(schemaVersion, profileId, tenantId, status, metadata, identities, credentialBindings, ResourceLimitPolicy.inheritAll());
    }

    private static Set<String> uniqueIdentityIds(List<AuthenticatedIdentity> identities) {
        Set<String> identityIds = new HashSet<>();
        for (AuthenticatedIdentity identity : identities) {
            if (!identityIds.add(identity.id())) {
                throw new IllegalArgumentException("identities must have unique ids");
            }
        }
        return identityIds;
    }
}
