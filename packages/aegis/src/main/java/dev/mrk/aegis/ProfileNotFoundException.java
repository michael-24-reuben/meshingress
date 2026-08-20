package dev.mrk.aegis;

import java.util.UUID;

/** The requested profile does not exist in the selected profile store. */
public final class ProfileNotFoundException extends IllegalArgumentException {
    public ProfileNotFoundException(UUID profileId) {
        super("unknown profile: " + profileId);
    }
}
