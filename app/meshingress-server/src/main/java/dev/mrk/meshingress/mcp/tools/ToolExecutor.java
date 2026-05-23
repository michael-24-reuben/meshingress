package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.api.McpCallContext;

public interface ToolExecutor {

    DispatchExecutionResult execute(String toolName, ObjectNode arguments, McpCallContext context);
}
