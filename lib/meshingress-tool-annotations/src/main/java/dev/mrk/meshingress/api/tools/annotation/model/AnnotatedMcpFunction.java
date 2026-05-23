package dev.mrk.meshingress.api.tools.annotation.model;

import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailability;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.util.List;

public record AnnotatedMcpFunction(
        String name,
        String path,
        String title,
        String description,
        McpFunctionAvailability availability,
        Method method,
        ObjectNode inputSchema,
        ObjectNode annotations,
        List<AnnotatedMcpFunctionParam> parameters
) {
    public AnnotatedMcpFunction {
        parameters = List.copyOf(parameters);
    }

    /**
     * Transitional convenience accessor.
     * Remove once all callers use availability().enabled().
     */
    public boolean enabled() {
        return availability.enabled();
    }

    /**
     * Transitional convenience accessor.
     * Remove once all callers use availability().visibility().
     */
    public ToolVisibility visibility() {
        return availability.visibility();
    }
}
