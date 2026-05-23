package dev.mrk.meshingress.api.tools;

import dev.mrk.meshingress.api.McpDispatchHandler;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.api.McpCallContext;

public interface McpToolHandler extends McpDispatchHandler<DispatchExecutionResult> {
    McpToolDescriptor descriptor();
}
