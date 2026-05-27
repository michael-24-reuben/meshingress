package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.mcp.tools.ToolRegistry;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.registry.ToolModuleRegistration;
import dev.mrk.meshingress.runtime.registry.ToolRegistrationBridge;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuntimeToolRegistryBridge implements ToolRegistrationBridge {

    private final ToolRegistry toolRegistry;

    public RuntimeToolRegistryBridge(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ToolModuleRegistration register(ToolModuleId moduleId, List<McpToolHandler> handlers) {
        List<String> registeredFunctions = new ArrayList<>();
        try {
            for (McpToolHandler handler : handlers) {
                toolRegistry.registerRuntimeHandler(handler, moduleId.value());
                handler.descriptor().functions()
                        .forEach(function -> registeredFunctions.add(function.name()));
            }
            return new ToolModuleRegistration(registeredFunctions);
        } catch (RuntimeException exception) {
            toolRegistry.unregisterRuntimeOwner(moduleId.value());
            throw exception;
        }
    }

    @Override
    public void unregister(ToolModuleId moduleId) {
        toolRegistry.unregisterRuntimeOwner(moduleId.value());
    }
}
