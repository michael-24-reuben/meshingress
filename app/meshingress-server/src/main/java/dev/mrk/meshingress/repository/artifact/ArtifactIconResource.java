package dev.mrk.meshingress.repository.artifact;

import java.nio.file.Path;

/** A safe, manifest-declared icon resource prepared for Studio or repository clients. */
public record ArtifactIconResource(Path path, String mimeType, String accessibleLabel) {
}
