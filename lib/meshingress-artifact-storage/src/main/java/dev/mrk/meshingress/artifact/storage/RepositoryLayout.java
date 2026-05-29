package dev.mrk.meshingress.artifact.storage;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;

import java.nio.file.Path;

public record RepositoryLayout(Path root) {
    public RepositoryLayout {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        root = root.toAbsolutePath().normalize();
    }

    public Path artifactsRoot() {
        return root.resolve("artifacts");
    }

    public Path metadataRoot() {
        return root.resolve("metadata");
    }

    public Path indexesRoot() {
        return root.resolve("indexes");
    }

    public Path quarantineRoot() {
        return root.resolve("quarantine");
    }

    public Path assessmentsRoot() {
        return root.resolve("assessments");
    }

    public Path reviewsRoot() {
        return root.resolve("reviews");
    }

    public Path sbomRoot() {
        return root.resolve("sbom");
    }

    public Path attestationsRoot() {
        return root.resolve("attestations");
    }

    public Path signaturesRoot() {
        return root.resolve("signatures");
    }

    public Path publicationsRoot() {
        return root.resolve("publications");
    }

    public Path artifactDirectory(ArtifactCoordinate coordinate) {
        return coordinatePath(artifactsRoot(), coordinate);
    }

    public Path metadataDirectory(ArtifactCoordinate coordinate) {
        return coordinatePath(metadataRoot(), coordinate);
    }

    public Path quarantineDirectory(ArtifactCoordinate coordinate) {
        return coordinatePath(quarantineRoot(), coordinate);
    }

    public Path assessmentDirectory(ArtifactCoordinate coordinate) {
        return coordinatePath(assessmentsRoot(), coordinate);
    }

    public Path reviewDirectory(ArtifactCoordinate coordinate) {
        return coordinatePath(reviewsRoot(), coordinate);
    }

    public Path publicationDirectory(ArtifactCoordinate coordinate) {
        return coordinatePath(publicationsRoot(), coordinate);
    }

    private Path coordinatePath(Path base, ArtifactCoordinate coordinate) {
        Path current = base;
        for (String segment : coordinate.storageSegments()) {
            current = current.resolve(segment);
        }
        return current.normalize();
    }
}
