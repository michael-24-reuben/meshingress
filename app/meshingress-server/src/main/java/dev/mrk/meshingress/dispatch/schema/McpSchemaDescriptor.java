package dev.mrk.meshingress.dispatch.schema;

public record McpSchemaDescriptor(
        String method,
        String location,
        Class<?> schemaClass
) {
}
