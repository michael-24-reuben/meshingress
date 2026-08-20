package dev.mrk.aegis;

/** Lifecycle state of an authentication profile. */
public enum ProfileStatus {
    PENDING_REVIEW,
    ACTIVE,
    DISABLED,
    REVOKED
}
