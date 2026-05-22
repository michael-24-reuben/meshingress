package dev.mrk.meshingress.api.tools.annotation.model;

import dev.mrk.meshingress.api.tools.McpToolDescriptor;

import java.util.List;
import java.util.Optional;

//         Class<?> controllerType,
//        Method handlerMethod,
//        McpRoute route,
//        McpConfigureMapping configuration,
//        List<Class<? extends McpMiddleware<?, ?, ?>>> middlewareTypes,
//        List<Annotation> availabilityAnnotations
public record AnnotatedMcpTool(
        Class<?> toolClass,
        String mapping,
        String invocationName,
        McpToolDescriptor descriptor,
        List<AnnotatedMcpFunction> functions
) {
    public AnnotatedMcpTool {
        functions = List.copyOf(functions);
    }

    public Optional<AnnotatedMcpFunction> defaultFunction() {
        if (invocationName == null || invocationName.isBlank()) {
            return functions.stream().findFirst();
        }
        return functions.stream()
                .filter(function -> invocationName.equals(function.name())
                        || invocationName.equals(function.path())
                        || invocationName.equals(descriptor.name()))
                .findFirst()
                .or(() -> functions.stream().findFirst());
    }
}
