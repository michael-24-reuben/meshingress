package dev.mrk.meshingress.api.tools;

import dev.mrk.meshingress.api.McpDispatchHandler;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.api.McpCallContext;

public interface McpToolHandler extends McpDispatchHandler<ToolExecutionResult> {
    McpToolDescriptor descriptor();
}
