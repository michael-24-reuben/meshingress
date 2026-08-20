package dev.mrk.aegis;

/** Validates host-defined credential evidence and produces a verified grant set. */
@FunctionalInterface
public interface CredentialVerifier {
    CredentialVerification verify(Credential credential);
}
