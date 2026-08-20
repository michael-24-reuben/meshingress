package dev.mrk.meshingress.toolmetadata;

import java.util.Comparator;
import java.util.List;

/** Static manifest payload. Schema two moves authored identity into {@link ToolModuleMetadata}. */
public record McpToolNativeMetadata(
        int schemaVersion,
        ToolModuleMetadata metadata,
        List<ToolProperty> properties,
        List<ToolRequirement> requirements,
        ToolReadme readme
) {
    public McpToolNativeMetadata(List<ToolProperty> properties, ToolReadme readme) {
        this(McpToolManifestJson.SCHEMA_VERSION, ToolModuleMetadata.empty(), properties, List.of(), readme);
    }

    public McpToolNativeMetadata {
        schemaVersion = schemaVersion <= 0 ? McpToolManifestJson.SCHEMA_VERSION : schemaVersion;
        metadata = metadata == null ? ToolModuleMetadata.empty() : metadata;
        properties = properties == null ? List.of() : properties.stream().sorted(Comparator.comparing(ToolProperty::name)).toList();
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        readme = readme == null ? ToolReadme.none() : readme;
    }

    /** Schema-one compatibility constructor. New writes use {@code metadata}. */
    public McpToolNativeMetadata(int schemaVersion, String toolId, List<ToolProperty> properties,
                                 List<ToolRequirement> requirements, List<ToolLink> links, ToolReadme readme) {
        this(schemaVersion, new ToolModuleMetadata(legacyNamespace(toolId), "", "", "", List.of(), "", List.of(), links, null),
                properties, requirements, readme);
    }

    public static McpToolNativeMetadata fromManifest(McpToolManifestDefinition definition) {
        if (definition == null) return empty();
        return new McpToolNativeMetadata(McpToolManifestJson.SCHEMA_VERSION, definition.metadata(),
                definition.properties(), definition.requirements(), definition.readme());
    }

    public static McpToolNativeMetadata empty() {
        return new McpToolNativeMetadata(List.of(), ToolReadme.none());
    }

    public boolean hasProperties() { return !properties.isEmpty(); }
    public boolean hasRequirements() { return !requirements.isEmpty(); }
    public boolean hasLinks() { return !metadata.links().isEmpty(); }
    public boolean hasReadme() { return readme.getMetadata().type() != ToolReadme.ReadmeType.None; }
    public boolean hasMetadata() { return !metadata.namespace().isBlank(); }

    /** Legacy read view; callers should use {@link #metadata()}. */
    public String toolId() { return metadata.namespace(); }
    /** Legacy read view; callers should use {@link ToolModuleMetadata#links()}. */
    public List<ToolLink> links() { return metadata.links(); }

    private static String legacyNamespace(String toolId) {
        if (toolId == null || toolId.isBlank()) return "";
        String normalized = toolId.strip();
        int delimiter = normalized.indexOf('.');
        return delimiter < 0 ? normalized : normalized.substring(0, delimiter);
    }
}
