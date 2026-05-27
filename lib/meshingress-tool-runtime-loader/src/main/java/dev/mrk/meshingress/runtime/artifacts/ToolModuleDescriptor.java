package dev.mrk.meshingress.runtime.artifacts;

import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;

import java.util.List;
import java.util.Map;

public record ToolModuleDescriptor(
        ToolModuleId moduleId,
        String displayName,
        String version,
        List<String> autoConfigurationClasses,
        Map<String, String> metadata
) {
    public ToolModuleDescriptor {
        autoConfigurationClasses = autoConfigurationClasses == null ? List.of() : List.copyOf(autoConfigurationClasses);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
