package dev.mrk.meshingress.dispatch.invoker;

import dev.mrk.meshingress.dispatch.schema.McpSchemaDescriptor;
import dev.mrk.meshingress.dispatch.schema.McpSchemaRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class McpDispatchRegistry {

    private final Map<String, McpDispatchHandlerMethod> handlers;
    private final McpSchemaRegistry schemaRegistry;

    public McpDispatchRegistry(Map<String, McpDispatchHandlerMethod> handlers) {
        this.handlers = Map.copyOf(handlers);
        this.schemaRegistry = new McpSchemaRegistry(groupSchemas(handlers));
    }

    public Optional<McpDispatchHandlerMethod> find(String method) {
        return Optional.ofNullable(handlers.get(method));
    }

    public Set<String> supportedMethods() {
        return handlers.keySet();
    }

    public McpSchemaRegistry schemaRegistry() {
        return schemaRegistry;
    }

    private Map<String, List<McpSchemaDescriptor>> groupSchemas(Map<String, McpDispatchHandlerMethod> handlers) {
        Map<String, List<McpSchemaDescriptor>> grouped = new LinkedHashMap<>();
        for (McpDispatchHandlerMethod handler : handlers.values()) {
            grouped.put(handler.methodName(), handler.schemas().stream().collect(Collectors.toList()));
        }
        return grouped;
    }
}
