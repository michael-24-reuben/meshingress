package dev.mrk.aegis;

/** Verifies host-defined bootstrap evidence without retaining it in Aegis state. */
@FunctionalInterface
public interface BootstrapCredentialVerifier {
    BootstrapCredentialVerification verify(Credential credential);
}
