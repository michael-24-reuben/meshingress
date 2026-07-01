package dev.mrk.meshingress.toolmetadata;

import java.util.Comparator;
import java.util.List;

public record McpToolNativeMetadata(
        List<McpToolPropertyDefinition> properties,
        String readme
) {
    public McpToolNativeMetadata {
        properties = properties == null
                ? List.of()
                : properties.stream()
                .sorted(Comparator.comparing(McpToolPropertyDefinition::name))
                .toList();
        readme = readme == null ? "" : readme.strip();
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public boolean hasReadme() {
        return !readme.isBlank();
    }
}
