package dev.mrk.meshingress.toolmetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class McpToolNativeMetadataExporter {

    private static final String RESOURCES_DIRECTORY = "resources";
    private static final String META_INF_DIRECTORY = "META-INF/meshingress";

    public void export(McpToolNativeMetadata metadata, Path artifactDirectory) {
        export(metadata, artifactDirectory, null);
    }

    /** Exports metadata and copies a declared, validated artifact-local icon when available. */
    public void export(McpToolNativeMetadata metadata, Path artifactDirectory, Path extractedArtifactDirectory) {
        if (metadata == null || artifactDirectory == null) {
            return;
        }
        try {
            Files.createDirectories(artifactDirectory);
            Path resourcesDirectory = artifactDirectory.resolve(RESOURCES_DIRECTORY);
            Files.createDirectories(resourcesDirectory);
            if (metadata.hasProperties() || metadata.hasRequirements() || metadata.hasLinks() || metadata.hasMetadata()) {
                McpToolManifestJson.write(metadata, artifactDirectory.resolve(McpToolManifestJson.RESOURCES_MANIFEST_PATH));
                McpToolManifestJson.write(metadata, artifactDirectory.resolve(META_INF_DIRECTORY).resolve("tool-manifest.json"));
            }
            if (metadata.hasProperties()) {
                Files.writeString(resourcesDirectory.resolve("application.properties"), toApplicationProperties(metadata));
            }
            if (metadata.hasReadme()) {
                String resolvedReadme = new ToolReadmeResolver().resolve(metadata.readme());
                Files.writeString(artifactDirectory.resolve("README.md"), resolvedReadme);
                Files.writeString(resourcesDirectory.resolve("README.md"), resolvedReadme);
            }
            copyDeclaredIcon(metadata, resourcesDirectory, extractedArtifactDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export native tool metadata: " + exception.getMessage(), exception);
        }
    }

    private void copyDeclaredIcon(McpToolNativeMetadata metadata, Path resourcesDirectory, Path extractedArtifactDirectory) throws Exception {
        ToolIcon icon = metadata.metadata().icon();
        if (icon == null || extractedArtifactDirectory == null) return;
        Path source = icon.requireValidResource(extractedArtifactDirectory);
        Path target = icon.resolveWithin(resourcesDirectory);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        icon.requireValidResource(resourcesDirectory);
    }

    private String toApplicationProperties(McpToolNativeMetadata metadata) {
        StringBuilder properties = new StringBuilder();
        properties.append("# Inferred Meshingress native tool properties.").append(System.lineSeparator());
        properties.append("# Edit values here for native/runtime references instead of hard-coding them in server defaults.")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        for (ToolProperty property : metadata.properties()) {
            properties.append("# ").append(property.description()).append(System.lineSeparator());
            properties.append("# type: ").append(property.valueType()).append("; required: ").append(property.required())
                    .append(System.lineSeparator());
            properties.append(property.name()).append('=').append(toPropertyValue(property))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return properties.toString();
    }

    private String toPropertyValue(ToolProperty property) {
        String value = property.defaultValue() == null ? "" : property.defaultValue();
        if (value.isEmpty()) {
            return "";
        }
        return switch (property.valueType().toLowerCase(java.util.Locale.ROOT)) {
            case "boolean" -> Boolean.toString(parseBoolean(property, value));
            case "long", "integer" -> Long.toString(parseLong(property, value));
            case "number", "double", "decimal" -> parseNumber(property, value).toPlainString();
            case "integer-list" -> joinIntegerList(property, value);
            case "number-list" -> joinNumberList(property, value);
            default -> escapePropertyValue(value);
        };
    }

    private boolean parseBoolean(ToolProperty property, String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        throw invalidDefault(property, "boolean");
    }

    private long parseLong(ToolProperty property, String value) {
        try {
            return Long.parseLong(value.strip());
        } catch (NumberFormatException exception) {
            throw invalidDefault(property, "long");
        }
    }

    private BigDecimal parseNumber(ToolProperty property, String value) {
        try {
            return new BigDecimal(value.strip());
        } catch (NumberFormatException exception) {
            throw invalidDefault(property, "number");
        }
    }

    private String joinIntegerList(ToolProperty property, String value) {
        List<String> converted = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            converted.add(Long.toString(parseLong(property, item)));
        }
        return String.join(",", converted);
    }

    private String joinNumberList(ToolProperty property, String value) {
        List<String> converted = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            converted.add(parseNumber(property, item).toPlainString());
        }
        return String.join(",", converted);
    }

    private IllegalArgumentException invalidDefault(ToolProperty property, String expectedType) {
        return new IllegalArgumentException("Tool property " + property.name()
                + " has a default value incompatible with declared " + expectedType + " type.");
    }

    private String escapePropertyValue(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\f' -> escaped.append("\\f");
                case '=', ':', '#', '!' -> escaped.append('\\').append(character);
                case ' ' -> {
                    if (index == 0) {
                        escaped.append('\\');
                    }
                    escaped.append(character);
                }
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
