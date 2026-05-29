package dev.mrk.meshingress.artifact.model;

public record ArtifactFileEntry(
        String path,
        long size,
        ArtifactChecksum checksum,
        boolean executable
) {
    public ArtifactFileEntry {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        if (checksum == null) {
            throw new IllegalArgumentException("checksum must not be null");
        }
    }
}
