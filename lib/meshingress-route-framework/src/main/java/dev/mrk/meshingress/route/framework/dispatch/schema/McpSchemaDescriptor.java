package dev.mrk.meshingress.route.framework.dispatch.schema;

public record McpSchemaDescriptor(
        String method,
        String location,
        Class<?> schemaClass
) {
}
