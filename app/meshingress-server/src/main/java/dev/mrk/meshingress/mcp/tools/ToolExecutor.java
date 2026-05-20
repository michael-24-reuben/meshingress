package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.tools.ToolExecutionResult;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.api.McpCallContext;

public interface ToolExecutor {

    ToolExecutionResult execute(String toolName, ObjectNode arguments, McpCallContext context);
}
