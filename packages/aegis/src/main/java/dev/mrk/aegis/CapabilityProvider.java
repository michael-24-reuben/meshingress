package dev.mrk.aegis;

/** Creates an operation surface after Aegis has verified the credential and grants. */
@FunctionalInterface
public interface CapabilityProvider<C extends Capability> {
    C create(VerifiedCredential credential);
}
