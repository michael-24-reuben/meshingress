package dev.mrk.meshingress.mcp.tools.registry;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;

import java.util.List;
import java.util.Optional;

public interface ToolRegistry {

    List<McpToolDescriptor> listPublicEnabledTools();

    List<McpFunctionDescriptor> listPublicEnabledFunctions();

    List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate);

    Optional<McpToolDescriptor> findEnabledTool(String name);

    Optional<McpFunctionDescriptor> findEnabledFunction(String name);

    Optional<McpToolDescriptor> findOwningTool(String functionName);

    /** Optional opaque module catalog identity for a callable function. */
    default Optional<String> findOwningModuleToolId(String functionName) {
        return Optional.empty();
    }

    Optional<McpToolDescriptor> findTool(String name);

    Optional<McpToolHandler> findHandler(String handlerKey);

    ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode);

    McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context);

    McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner);

    void unregisterRuntimeOwner(String owner);

    McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context);

    McpToolDescriptor disable(String name, McpCallContext context);

    long registryVersion();

    List<ToolAuditEvent> auditEvents();
}
