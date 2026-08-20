package dev.mrk.aegis;

import java.util.Objects;

/** Immutable stored-profile view with a host-managed optimistic-lock revision. */
public record ProfileSnapshot(AuthProfile profile, long revision) {
    public ProfileSnapshot {
        profile = Objects.requireNonNull(profile, "profile");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
