package dev.mrk.aegis;

/** Durable lifecycle state for safe bootstrap issuance metadata. */
public enum BootstrapIssuanceState {
    ACTIVE,
    CONSUMED,
    REVOKED,
    EXHAUSTED
}
