package dev.mrk.aegis;

import java.util.Objects;
import java.util.Set;

/** Bounded query understood by a profile store. An empty status set means every status. */
public record ProfileQuery(Set<ProfileStatus> statuses, int limit) {
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAXIMUM_LIMIT = 1_000;

    public ProfileQuery {
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses"));
        if (limit < 1 || limit > MAXIMUM_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAXIMUM_LIMIT);
        }
    }

    public static ProfileQuery all() {
        return new ProfileQuery(Set.of(), DEFAULT_LIMIT);
    }
}
