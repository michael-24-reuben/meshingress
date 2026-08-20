package dev.mrk.aegis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Thread-safe reference store for isolated tests and local demonstrations only. */
public final class InMemoryProfileStore implements ProfileStore {
    private final Map<UUID, ProfileSnapshot> profiles = new HashMap<>();

    @Override
    public synchronized ProfileSnapshot create(AuthProfile profile) {
        Objects.requireNonNull(profile, "profile");
        ProfileSnapshot snapshot = new ProfileSnapshot(profile, 0);
        if (profiles.putIfAbsent(profile.profileId(), snapshot) != null) {
            throw new IllegalArgumentException("profile already exists: " + profile.profileId());
        }
        return snapshot;
    }

    @Override
    public synchronized Optional<ProfileSnapshot> find(UUID profileId) {
        return Optional.ofNullable(profiles.get(Objects.requireNonNull(profileId, "profileId")));
    }

    @Override
    public synchronized List<ProfileSnapshot> list(ProfileQuery query) {
        Objects.requireNonNull(query, "query");
        List<ProfileSnapshot> result = new ArrayList<>(profiles.values());
        result.removeIf(snapshot -> !query.statuses().isEmpty()
                && !query.statuses().contains(snapshot.profile().status()));
        result.sort(Comparator.comparing(snapshot -> snapshot.profile().profileId()));
        return List.copyOf(result.subList(0, Math.min(query.limit(), result.size())));
    }

    @Override
    public synchronized ProfileSnapshot replace(UUID profileId, long expectedRevision, AuthProfile replacement) {
        profileId = Objects.requireNonNull(profileId, "profileId");
        replacement = Objects.requireNonNull(replacement, "replacement");
        if (!profileId.equals(replacement.profileId())) {
            throw new IllegalArgumentException("replacement profile id must match profileId");
        }
        ProfileSnapshot existing = profiles.get(profileId);
        if (existing == null) {
            throw new ProfileNotFoundException(profileId);
        }
        if (existing.revision() != expectedRevision) {
            throw new ProfileRevisionConflictException(profileId, expectedRevision, existing.revision());
        }
        ProfileSnapshot updated = new ProfileSnapshot(replacement, Math.addExact(expectedRevision, 1));
        profiles.put(profileId, updated);
        return updated;
    }
}
