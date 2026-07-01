package dev.mrk.meshingress.artifact.publication;

public record PublicationSignature(String keyId, String algorithm, String value) {
    public PublicationSignature(String algorithm, String value) {
        this("", algorithm, value);
    }

    public PublicationSignature {
        keyId = keyId == null ? "" : keyId.trim();
        algorithm = algorithm == null ? "" : algorithm.trim();
        value = value == null ? "" : value.trim();
    }
}
