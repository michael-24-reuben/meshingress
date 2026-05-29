package dev.mrk.meshingress.artifact.security;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactFileEntry;

import java.nio.file.Path;
import java.util.List;

public record ScannerRequest(
        ArtifactCoordinate coordinate,
        Path artifactPath,
        Path quarantinePath,
        List<ArtifactFileEntry> fileEntries
) {
    public ScannerRequest {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate must not be null");
        }
        if (artifactPath == null) {
            throw new IllegalArgumentException("artifactPath must not be null");
        }
        fileEntries = fileEntries == null ? List.of() : List.copyOf(fileEntries);
    }
}
