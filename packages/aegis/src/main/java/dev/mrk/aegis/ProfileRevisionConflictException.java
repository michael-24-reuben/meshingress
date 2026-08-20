package dev.mrk.aegis;

import java.util.UUID;

/** A profile was changed after the caller read the revision it supplied. */
public final class ProfileRevisionConflictException extends IllegalStateException {
    private final UUID profileId;
    private final long expectedRevision;
    private final long actualRevision;

    public ProfileRevisionConflictException(UUID profileId, long expectedRevision, long actualRevision) {
        super("profile revision conflict for " + profileId + ": expected " + expectedRevision + ", actual " + actualRevision);
        this.profileId = profileId;
        this.expectedRevision = expectedRevision;
        this.actualRevision = actualRevision;
    }

    public UUID profileId() {
        return profileId;
    }

    public long expectedRevision() {
        return expectedRevision;
    }

    public long actualRevision() {
        return actualRevision;
    }
}
