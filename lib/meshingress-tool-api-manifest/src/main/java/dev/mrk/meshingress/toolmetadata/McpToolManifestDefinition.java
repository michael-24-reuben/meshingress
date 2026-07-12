package dev.mrk.meshingress.toolmetadata;

import java.util.List;

public interface McpToolManifestDefinition {

    String toolId();

    default List<ToolProperty> properties() {
        return List.of();
    }

    default List<ToolRequirement> requirements() {
        return List.of();
    }

    default List<ToolLink> links() {
        return List.of();
    }

    default String readmeMarkdown() {
        return "";
    }
}
