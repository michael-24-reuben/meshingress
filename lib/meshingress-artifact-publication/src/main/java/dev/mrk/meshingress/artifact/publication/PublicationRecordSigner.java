package dev.mrk.meshingress.artifact.publication;

public interface PublicationRecordSigner {
    PublicationSignature sign(String payload);
}
