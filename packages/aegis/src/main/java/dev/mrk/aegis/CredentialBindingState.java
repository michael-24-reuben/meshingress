package dev.mrk.aegis;

/** State of an upstream credential binding without revealing its secret. */
public enum CredentialBindingState {
    ACTIVE,
    EXPIRED,
    REVOKED,
    REAUTHORIZATION_REQUIRED
}
