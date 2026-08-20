package dev.mrk.meshingress.toolmetadata;

import java.util.List;

public interface McpToolManifestDefinition {

    ToolModuleMetadata metadata();

    default List<ToolProperty> properties() {
        return List.of();
    }

    default List<ToolRequirement> requirements() {
        return List.of();
    }

    default ToolReadme readme() {
        return ToolReadme.none();
    }
}
