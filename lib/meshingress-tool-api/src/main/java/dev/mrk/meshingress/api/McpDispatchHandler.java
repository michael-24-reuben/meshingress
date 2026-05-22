package dev.mrk.meshingress.api;

import tools.jackson.databind.node.ObjectNode;

public interface McpDispatchHandler <R extends DispatchExecutionResult> {
    R call(ObjectNode arguments, McpCallContext context);
}
