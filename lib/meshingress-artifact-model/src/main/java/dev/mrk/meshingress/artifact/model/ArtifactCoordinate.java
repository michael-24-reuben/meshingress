package dev.mrk.meshingress.artifact.model;

import java.util.List;

public record ArtifactCoordinate(
        String groupId,
        String artifactId,
        String version,
        String classifier,
        String packaging
) {
    public ArtifactCoordinate {
        groupId = requireCoordinatePart(groupId, "groupId");
        artifactId = requireCoordinatePart(artifactId, "artifactId");
        version = requireCoordinatePart(version, "version");
        classifier = classifier == null || classifier.isBlank() ? null : classifier.trim();
        packaging = packaging == null || packaging.isBlank() ? "jar" : packaging.trim();
    }

    public String gav() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String display() {
        String suffix = classifier == null ? "" : ":" + classifier;
        return gav() + suffix + "@" + packaging;
    }

    public List<String> storageSegments() {
        return List.of(groupId.replace('.', '/'), artifactId, version);
    }

    private static String requireCoordinatePart(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new IllegalArgumentException(name + " contains invalid path characters");
        }
        return trimmed;
    }
}
