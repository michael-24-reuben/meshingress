package dev.mrk.meshingress.dispatch.invoker;

import dev.mrk.meshingress.dispatch.schema.McpSchemaDescriptor;

import java.lang.reflect.Method;
import java.util.List;

public record McpDispatchHandlerMethod(
        String methodName,
        Object bean,
        Method method,
        List<McpSchemaDescriptor> schemas
) {
}
