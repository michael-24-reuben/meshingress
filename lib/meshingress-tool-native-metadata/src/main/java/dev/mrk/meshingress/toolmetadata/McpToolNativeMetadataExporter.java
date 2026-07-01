package dev.mrk.meshingress.toolmetadata;

import java.nio.file.Files;
import java.nio.file.Path;

public class McpToolNativeMetadataExporter {

    private static final String RESOURCES_DIRECTORY = "resources";

    public void export(McpToolNativeMetadata metadata, Path artifactDirectory) {
        if (metadata == null || artifactDirectory == null) {
            return;
        }
        try {
            Files.createDirectories(artifactDirectory);
            Path resourcesDirectory = artifactDirectory.resolve(RESOURCES_DIRECTORY);
            Files.createDirectories(resourcesDirectory);
            if (metadata.hasProperties()) {
                Files.writeString(resourcesDirectory.resolve("application.yaml"), toApplicationYaml(metadata));
            }
            if (metadata.hasReadme()) {
                String readme = metadata.readme() + System.lineSeparator();
                Files.writeString(artifactDirectory.resolve("README.md"), readme);
                Files.writeString(resourcesDirectory.resolve("README.md"), readme);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export native tool metadata: " + exception.getMessage(), exception);
        }
    }

    private String toApplicationYaml(McpToolNativeMetadata metadata) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Inferred Meshingress native tool properties.").append(System.lineSeparator());
        yaml.append("# Edit values here for native/runtime references instead of hard-coding them in server defaults.")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        for (McpToolPropertyDefinition property : metadata.properties()) {
            yaml.append("# ").append(property.description()).append(System.lineSeparator());
            yaml.append("# type: ").append(property.valueType()).append("; required: ").append(property.required())
                    .append(System.lineSeparator());
            yaml.append(property.name()).append(": ").append(quoteYaml(property.defaultValue()))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return yaml.toString();
    }

    private String quoteYaml(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
