package dev.mrk.aegis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Host persistence boundary for normal profiles. {@link #replace} must perform
 * its expected-revision comparison and write atomically.
 */
public interface ProfileStore {
    ProfileSnapshot create(AuthProfile profile);

    Optional<ProfileSnapshot> find(UUID profileId);

    List<ProfileSnapshot> list(ProfileQuery query);

    ProfileSnapshot replace(UUID profileId, long expectedRevision, AuthProfile replacement);
}
