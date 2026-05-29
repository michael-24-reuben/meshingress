package dev.mrk.meshingress.artifact.publication;

public record PublicationSignature(String algorithm, String value) {
    public PublicationSignature {
        algorithm = algorithm == null ? "" : algorithm.trim();
        value = value == null ? "" : value.trim();
    }
}
