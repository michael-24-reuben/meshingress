package dev.mrk.meshingress.runtime.bundle;

import java.util.List;
import java.util.Objects;

public record ClasspathToolBundle(
        String artifactId,
        List<String> toolArtifacts,
        ToolLoadingStrategy strategy,
        String description
) {
    public ClasspathToolBundle {
        Objects.requireNonNull(artifactId, "artifactId must not be null");
        if (artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }
        toolArtifacts = toolArtifacts == null ? List.of() : List.copyOf(toolArtifacts);
        strategy = strategy == null ? ToolLoadingStrategy.CLASSPATH_BUNDLE : strategy;
        if (strategy != ToolLoadingStrategy.CLASSPATH_BUNDLE) {
            throw new IllegalArgumentException("ClasspathToolBundle must use CLASSPATH_BUNDLE strategy");
        }
    }

    public static ClasspathToolBundle meshingressDefault(List<String> toolArtifacts) {
        return new ClasspathToolBundle(
                "meshingress-tool-distribution",
                toolArtifacts,
                ToolLoadingStrategy.CLASSPATH_BUNDLE,
                "Tools are resolved by Maven into a bundle dependency and discovered from the server classpath at startup."
        );
    }
}
