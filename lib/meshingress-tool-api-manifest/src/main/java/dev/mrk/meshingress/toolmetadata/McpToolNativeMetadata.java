package dev.mrk.meshingress.toolmetadata;

import java.util.Comparator;
import java.util.List;

public record McpToolNativeMetadata(
        int schemaVersion,
        String toolId,
        List<ToolProperty> properties,
        List<ToolRequirement> requirements,
        List<ToolLink> links,
        ToolReadme readme
) {
    public McpToolNativeMetadata(List<ToolProperty> properties, ToolReadme readme) {
        this(McpToolManifestJson.SCHEMA_VERSION, "", properties, List.of(), List.of(), readme);
    }

    public McpToolNativeMetadata {
        schemaVersion = schemaVersion <= 0 ? McpToolManifestJson.SCHEMA_VERSION : schemaVersion;
        toolId = toolId == null ? "" : toolId.strip();
        properties = properties == null
                ? List.of()
                : properties.stream()
                .sorted(Comparator.comparing(ToolProperty::name))
                .toList();
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        links = links == null ? List.of() : List.copyOf(links);
        readme = readme == null ? ToolReadme.none() : readme;
    }

    public static McpToolNativeMetadata fromManifest(McpToolManifestDefinition definition) {
        if (definition == null) {
            return empty();
        }
        return new McpToolNativeMetadata(
                McpToolManifestJson.SCHEMA_VERSION,
                definition.toolId(),
                definition.properties(),
                definition.requirements(),
                definition.links(),
                definition.readme()
        );
    }

    public static McpToolNativeMetadata empty() {
        return new McpToolNativeMetadata(List.of(), ToolReadme.none());
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public boolean hasRequirements() {
        return !requirements.isEmpty();
    }

    public boolean hasLinks() {
        return !links.isEmpty();
    }

    public boolean hasReadme() {
        return readme.getMetadata().type() != ToolReadme.ReadmeType.None;
    }
}
