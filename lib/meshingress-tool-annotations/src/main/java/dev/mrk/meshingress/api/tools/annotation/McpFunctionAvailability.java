package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.api.tools.ToolVisibility;

import java.util.List;

public record McpFunctionAvailability(
        boolean enabled,
        ToolVisibility visibility,
        McpAvailabilityMode mode,
        boolean available,
        List<McpAvailabilityConditionResult> conditions
) {
    public McpFunctionAvailability {
        visibility = visibility == null ? ToolVisibility.PUBLIC : visibility;
        mode = mode == null ? McpAvailabilityMode.ALL : mode;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);

        // Static disabled should always dominate dynamic condition success.
        available = enabled && available;
    }

    public static McpFunctionAvailability alwaysAvailable(boolean enabled, ToolVisibility visibility) {
        return new McpFunctionAvailability(
                enabled,
                visibility,
                McpAvailabilityMode.ALL,
                enabled,
                List.of()
        );
    }
}