package dev.mrk.meshingress.artifact.storage;

import dev.mrk.meshingress.artifact.model.ArtifactChecksum;

import java.nio.file.Path;

public record StoredArtifactBlob(
        Path path,
        String uri,
        ArtifactChecksum checksum,
        long size
) {
}
