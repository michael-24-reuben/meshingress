package dev.mrk.meshingress.api.tools.annotation.model;

import dev.mrk.meshingress.api.tools.McpToolDescriptor;

import java.util.List;
import java.util.Optional;

public record AnnotatedMcpTool(
        String name,
        Class<?> toolClass,
        String defaultFunctionName,
        McpToolDescriptor descriptor,
        List<AnnotatedMcpFunction> functions
) {
    public AnnotatedMcpTool {
        functions = List.copyOf(functions);
    }

    public Optional<AnnotatedMcpFunction> defaultFunction() {
        if (defaultFunctionName == null || defaultFunctionName.isBlank()) {
            return functions.stream().findFirst();
        }
        return functions.stream()
                .filter(function -> defaultFunctionName.equals(function.name())
                        || defaultFunctionName.equals(descriptor.name()))
                .findFirst()
                .or(() -> functions.stream().findFirst());
    }
}
