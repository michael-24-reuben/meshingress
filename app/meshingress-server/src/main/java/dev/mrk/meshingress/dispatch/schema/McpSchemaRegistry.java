package dev.mrk.meshingress.dispatch.schema;

import java.util.List;
import java.util.Map;

public class McpSchemaRegistry {

    private final Map<String, List<McpSchemaDescriptor>> descriptorsByMethod;

    public McpSchemaRegistry(Map<String, List<McpSchemaDescriptor>> descriptorsByMethod) {
        this.descriptorsByMethod = Map.copyOf(descriptorsByMethod);
    }

    public List<McpSchemaDescriptor> descriptorsFor(String method) {
        return descriptorsByMethod.getOrDefault(method, List.of());
    }
}
