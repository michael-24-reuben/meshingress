package dev.mrk.meshingress.artifact.model;

public record ArtifactProvenance(
        String sourceRepo,
        String sourceCommit,
        String generatedBy,
        String generatorCommit
) {
    public ArtifactProvenance {
        sourceRepo = normalize(sourceRepo);
        sourceCommit = normalize(sourceCommit);
        generatedBy = normalize(generatedBy);
        generatorCommit = normalize(generatorCommit);
    }

    public static ArtifactProvenance empty() {
        return new ArtifactProvenance("", "", "", "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
