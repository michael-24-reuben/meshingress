package dev.mrk.meshingress.api.tools.annotation.model;

import dev.mrk.meshingress.api.tools.ToolVisibility;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.util.List;

public record AnnotatedMcpFunction(
        String name,
        String path,
        String title,
        String description,
        boolean enabled,
        ToolVisibility visibility,
        Method method,
        ObjectNode inputSchema,
        ObjectNode annotations,
        List<AnnotatedMcpFunctionParam> parameters
) {
    public AnnotatedMcpFunction {
        parameters = List.copyOf(parameters);
    }
}
