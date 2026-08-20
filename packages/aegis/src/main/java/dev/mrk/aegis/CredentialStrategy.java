package dev.mrk.aegis;

/**
 * Explicit strategy a host uses when an authenticated identity accesses an
 * upstream target. A strategy never implies arbitrary credential forwarding.
 */
public enum CredentialStrategy {
    NONE,
    SECRET_REFERENCE,
    DELEGATED_OAUTH,
    TOKEN_EXCHANGE,
    HEADER_PASSTHROUGH,
    WORKLOAD_IDENTITY
}
