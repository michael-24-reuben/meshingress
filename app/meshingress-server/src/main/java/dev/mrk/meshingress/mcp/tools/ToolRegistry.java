package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolPatch;

import java.util.List;
import java.util.Optional;

public interface ToolRegistry {

    List<McpToolDescriptor> listPublicEnabledTools();

    List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate);

    Optional<McpToolDescriptor> findEnabledTool(String name);

    Optional<McpToolDescriptor> findTool(String name);

    Optional<McpToolHandler> findHandler(String handlerKey);

    ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode);

    McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context);

    McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context);

    McpToolDescriptor disable(String name, McpCallContext context);

    long registryVersion();

    List<ToolAuditEvent> auditEvents();
}
