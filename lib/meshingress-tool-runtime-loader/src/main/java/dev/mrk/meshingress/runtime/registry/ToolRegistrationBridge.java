package dev.mrk.meshingress.runtime.registry;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;

import java.util.List;

public interface ToolRegistrationBridge {
    ToolModuleRegistration register(ToolModuleId moduleId, List<McpToolHandler> handlers);

    void unregister(ToolModuleId moduleId);
}
