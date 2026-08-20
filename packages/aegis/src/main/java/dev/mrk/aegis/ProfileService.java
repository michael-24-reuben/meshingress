package dev.mrk.aegis;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Framework-free ordinary profile lifecycle service. All mutations are
 * compare-and-set operations and contain only credential-safe profile data.
 */
public final class ProfileService {
    private final ProfileStore store;
    private final Clock clock;
    private final Supplier<UUID> profileIds;

    public ProfileService(ProfileStore store) {
        this(store, Clock.systemUTC(), UUID::randomUUID);
    }

    public ProfileService(ProfileStore store, Clock clock, Supplier<UUID> profileIds) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.profileIds = Objects.requireNonNull(profileIds, "profileIds");
    }

    public ProfileSnapshot create(ProfileDraft draft) {
        return create(draft, ProfileStatus.ACTIVE);
    }

    /** Creates an unprivileged admission profile without allowing a revoked state. */
    public ProfileSnapshot create(ProfileDraft draft, ProfileStatus initialStatus) {
        draft = Objects.requireNonNull(draft, "draft");
        initialStatus = Objects.requireNonNull(initialStatus, "initialStatus");
        if (initialStatus == ProfileStatus.REVOKED) throw new IllegalArgumentException("a new profile cannot start revoked");
        Instant now = Instant.now(clock);
        AuthProfile profile = new AuthProfile(
                AuthProfile.CURRENT_SCHEMA_VERSION,
                Objects.requireNonNull(profileIds.get(), "profileIds returned null"),
                draft.tenantId(),
                initialStatus,
                new AuthProfileMetadata(draft.displayName(), now, now, draft.labels()),
                draft.identities(),
                draft.credentialBindings(),
                ResourceLimitPolicy.inheritAll()
        );
        return store.create(profile);
    }

    public ProfileSnapshot get(UUID profileId) {
        return store.find(Objects.requireNonNull(profileId, "profileId"))
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
    }

    public List<ProfileSnapshot> list(ProfileQuery query) {
        return store.list(Objects.requireNonNull(query, "query"));
    }

    public ProfileSnapshot updateMetadata(
            UUID profileId,
            long expectedRevision,
            String displayName,
            Map<String, String> labels
    ) {
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            return copy(profile, profile.status(), metadata(profile, displayName, labels),
                    profile.identities(), profile.credentialBindings(), profile.limitPolicy());
        });
    }

    public ProfileSnapshot updateLimitPolicy(UUID profileId, long expectedRevision, ResourceLimitPolicy limitPolicy) {
        limitPolicy = Objects.requireNonNull(limitPolicy, "limitPolicy");
        ResourceLimitPolicy requested = limitPolicy;
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            return copy(profile, profile.status(), metadata(profile), profile.identities(), profile.credentialBindings(), requested);
        });
    }

    public ProfileSnapshot activate(UUID profileId, long expectedRevision) {
        return mutate(profileId, expectedRevision, profile -> {
            if (profile.status() == ProfileStatus.REVOKED) throw new IllegalStateException("a revoked profile cannot be reactivated");
            if (profile.status() == ProfileStatus.ACTIVE) return profile;
            return copy(profile, ProfileStatus.ACTIVE, metadata(profile), profile.identities(), profile.credentialBindings(), profile.limitPolicy());
        });
    }

    public ProfileSnapshot disable(UUID profileId, long expectedRevision) {
        return changeStatus(profileId, expectedRevision, ProfileStatus.ACTIVE, ProfileStatus.DISABLED);
    }

    public ProfileSnapshot revoke(UUID profileId, long expectedRevision) {
        return mutate(profileId, expectedRevision, profile -> {
            if (profile.status() == ProfileStatus.REVOKED) {
                return profile;
            }
            List<CredentialBinding> revokedBindings = profile.credentialBindings().stream()
                    .map(binding -> binding.state() == CredentialBindingState.REVOKED ? binding : withState(binding, CredentialBindingState.REVOKED))
                    .toList();
            return copy(profile, ProfileStatus.REVOKED, metadata(profile), profile.identities(), revokedBindings, profile.limitPolicy());
        });
    }

    public ProfileSnapshot linkIdentity(UUID profileId, long expectedRevision, AuthenticatedIdentity identity) {
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            Objects.requireNonNull(identity, "identity");
            if (profile.identities().stream().anyMatch(existing -> existing.id().equals(identity.id()))) {
                throw new IllegalArgumentException("identity already exists: " + identity.id());
            }
            List<AuthenticatedIdentity> identities = new ArrayList<>(profile.identities());
            identities.add(identity);
            return copy(profile, profile.status(), metadata(profile), identities, profile.credentialBindings(), profile.limitPolicy());
        });
    }

    public ProfileSnapshot unlinkIdentity(UUID profileId, long expectedRevision, String identityId) {
        String requiredIdentityId = requireText(identityId, "identityId");
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            if (profile.identities().size() == 1 && profile.identities().getFirst().id().equals(requiredIdentityId)) {
                throw new IllegalStateException("a profile must retain at least one identity");
            }
            if (profile.credentialBindings().stream().anyMatch(binding -> binding.identityId().equals(requiredIdentityId))) {
                throw new IllegalStateException("identity has credential bindings and cannot be unlinked: " + requiredIdentityId);
            }
            List<AuthenticatedIdentity> identities = profile.identities().stream()
                    .filter(identity -> !identity.id().equals(requiredIdentityId))
                    .toList();
            if (identities.size() == profile.identities().size()) {
                throw new IllegalArgumentException("unknown identity: " + requiredIdentityId);
            }
            return copy(profile, profile.status(), metadata(profile), identities, profile.credentialBindings(), profile.limitPolicy());
        });
    }

    public ProfileSnapshot bindCredential(UUID profileId, long expectedRevision, CredentialBinding binding) {
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            Objects.requireNonNull(binding, "binding");
            if (profile.credentialBindings().stream().anyMatch(existing -> existing.id().equals(binding.id()))) {
                throw new IllegalArgumentException("credential binding already exists: " + binding.id());
            }
            List<CredentialBinding> bindings = new ArrayList<>(profile.credentialBindings());
            bindings.add(binding);
            return copy(profile, profile.status(), metadata(profile), profile.identities(), bindings, profile.limitPolicy());
        });
    }

    public ProfileSnapshot replaceCredential(
            UUID profileId,
            long expectedRevision,
            String bindingId,
            CredentialBinding replacement
    ) {
        String requiredBindingId = requireText(bindingId, "bindingId");
        replacement = Objects.requireNonNull(replacement, "replacement");
        CredentialBinding requiredReplacement = replacement;
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            if (!requiredBindingId.equals(requiredReplacement.id())) {
                throw new IllegalArgumentException("replacement binding id must match bindingId");
            }
            List<CredentialBinding> bindings = replaceBinding(profile.credentialBindings(), requiredBindingId, requiredReplacement);
            return copy(profile, profile.status(), metadata(profile), profile.identities(), bindings, profile.limitPolicy());
        });
    }

    public ProfileSnapshot revokeCredential(UUID profileId, long expectedRevision, String bindingId) {
        String requiredBindingId = requireText(bindingId, "bindingId");
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            List<CredentialBinding> bindings = replaceBinding(profile.credentialBindings(), requiredBindingId,
                    binding(profile.credentialBindings(), requiredBindingId, CredentialBindingState.REVOKED));
            return copy(profile, profile.status(), metadata(profile), profile.identities(), bindings, profile.limitPolicy());
        });
    }

    /** Detaches only Aegis metadata; it does not revoke or delete an external secret. */
    public ProfileSnapshot unbindCredential(UUID profileId, long expectedRevision, String bindingId) {
        String requiredBindingId = requireText(bindingId, "bindingId");
        return mutate(profileId, expectedRevision, profile -> {
            requireMutable(profile);
            List<CredentialBinding> bindings = profile.credentialBindings().stream()
                    .filter(binding -> !binding.id().equals(requiredBindingId))
                    .toList();
            if (bindings.size() == profile.credentialBindings().size()) {
                throw new IllegalArgumentException("unknown credential binding: " + requiredBindingId);
            }
            return copy(profile, profile.status(), metadata(profile), profile.identities(), bindings, profile.limitPolicy());
        });
    }

    private ProfileSnapshot changeStatus(UUID profileId, long expectedRevision, ProfileStatus from, ProfileStatus to) {
        return mutate(profileId, expectedRevision, profile -> {
            if (profile.status() == ProfileStatus.REVOKED) {
                throw new IllegalStateException("a revoked profile cannot be reactivated or disabled");
            }
            if (profile.status() == to) {
                return profile;
            }
            if (profile.status() != from) {
                throw new IllegalStateException("profile status must be " + from + " before changing to " + to);
            }
            return copy(profile, to, metadata(profile), profile.identities(), profile.credentialBindings(), profile.limitPolicy());
        });
    }

    private ProfileSnapshot mutate(UUID profileId, long expectedRevision, UnaryOperator<AuthProfile> operation) {
        ProfileSnapshot current = get(profileId);
        if (current.revision() != expectedRevision) {
            throw new ProfileRevisionConflictException(current.profile().profileId(), expectedRevision, current.revision());
        }
        AuthProfile updated = Objects.requireNonNull(operation.apply(current.profile()), "operation returned null");
        if (updated.equals(current.profile())) {
            return current;
        }
        return store.replace(current.profile().profileId(), expectedRevision, updated);
    }

    private AuthProfileMetadata metadata(AuthProfile profile) {
        AuthProfileMetadata current = profile.metadata();
        return new AuthProfileMetadata(current.displayName(), current.createdAt(), nextUpdatedAt(current), current.labels());
    }

    private AuthProfileMetadata metadata(AuthProfile profile, String displayName, Map<String, String> labels) {
        AuthProfileMetadata current = profile.metadata();
        return new AuthProfileMetadata(displayName, current.createdAt(), nextUpdatedAt(current), labels);
    }

    private Instant nextUpdatedAt(AuthProfileMetadata metadata) {
        Instant now = Instant.now(clock);
        return now.isAfter(metadata.updatedAt()) ? now : metadata.updatedAt();
    }

    private static void requireMutable(AuthProfile profile) {
        if (profile.status() == ProfileStatus.REVOKED) {
            throw new IllegalStateException("a revoked profile is terminal");
        }
    }

    private static AuthProfile copy(
            AuthProfile profile,
            ProfileStatus status,
            AuthProfileMetadata metadata,
            List<AuthenticatedIdentity> identities,
            List<CredentialBinding> bindings,
            ResourceLimitPolicy limitPolicy
    ) {
        return new AuthProfile(profile.schemaVersion(), profile.profileId(), profile.tenantId(), status, metadata, identities, bindings, limitPolicy);
    }

    private static CredentialBinding binding(List<CredentialBinding> bindings, String bindingId, CredentialBindingState state) {
        for (CredentialBinding binding : bindings) {
            if (binding.id().equals(bindingId)) {
                return withState(binding, state);
            }
        }
        throw new IllegalArgumentException("unknown credential binding: " + bindingId);
    }

    private static List<CredentialBinding> replaceBinding(
            List<CredentialBinding> bindings,
            String bindingId,
            CredentialBinding replacement
    ) {
        List<CredentialBinding> replaced = new ArrayList<>(bindings.size());
        boolean found = false;
        for (CredentialBinding binding : bindings) {
            if (binding.id().equals(bindingId)) {
                replaced.add(replacement);
                found = true;
            } else {
                replaced.add(binding);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("unknown credential binding: " + bindingId);
        }
        return replaced;
    }

    private static CredentialBinding withState(CredentialBinding binding, CredentialBindingState state) {
        return new CredentialBinding(
                binding.id(), binding.identityId(), binding.target(), binding.strategy(), binding.secretReference(),
                binding.grantedScopes(), state, binding.expiresAt()
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
