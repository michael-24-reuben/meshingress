package dev.mrk.meshingress.route.framework.dispatch;

import dev.mrk.meshingress.route.framework.dispatch.schema.McpSchemaDescriptor;

import java.lang.reflect.Method;
import java.util.List;

public record McpDispatchHandlerMethod(
        String methodName,
        Object bean,
        Method method,
        List<McpSchemaDescriptor> schemas
) {
}
