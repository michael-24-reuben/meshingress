package dev.mrk.meshingress.artifact.model;

public record ArtifactChecksum(String algorithm, String value) {
    public ArtifactChecksum {
        algorithm = requireText(algorithm, "algorithm").toUpperCase();
        value = requireText(value, "value").toLowerCase();
    }

    public static ArtifactChecksum sha256(String value) {
        return new ArtifactChecksum("SHA-256", value);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
