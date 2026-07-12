package dev.mrk.meshingress.repository.artifact.store;

import dev.mrk.meshingress.artifact.model.ArtifactRecord;

import java.nio.file.Path;

public record ArtifactMetadataEntry(
        ArtifactRecord record,
        Path artifactPath
) {
    public ArtifactMetadataEntry {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        if (artifactPath == null) {
            throw new IllegalArgumentException("artifactPath must not be null");
        }
    }
}
