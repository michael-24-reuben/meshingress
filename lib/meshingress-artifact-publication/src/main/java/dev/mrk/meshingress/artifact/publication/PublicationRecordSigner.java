package dev.mrk.meshingress.artifact.publication;

public interface PublicationRecordSigner {
    String keyId();

    String algorithm();

    PublicationSignature sign(String payload);
}
