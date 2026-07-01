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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Composite registry that delegates reads across ordered registry backends.
 * The first delegate remains the mutation target for register/update/disable operations.
 */
public class DelegatingToolRegistry implements ToolRegistry {

    private final List<ToolRegistry> delegates;

    public DelegatingToolRegistry(ToolRegistry delegate) {
        this(List.of(delegate));
    }

    public DelegatingToolRegistry(List<? extends ToolRegistry> delegates) {
        Objects.requireNonNull(delegates, "delegates");
        if (delegates.isEmpty()) {
            throw new IllegalArgumentException("At least one tool registry delegate is required.");
        }
        this.delegates = List.copyOf(delegates);
    }

    protected List<ToolRegistry> delegates() {
        return delegates;
    }

    protected ToolRegistry primary() {
        return delegates.getFirst();
    }

    @Override
    public List<McpToolDescriptor> listPublicEnabledTools() {
        Map<String, McpToolDescriptor> tools = new LinkedHashMap<>();
        for (ToolRegistry delegate : delegates) {
            for (McpToolDescriptor tool : delegate.listPublicEnabledTools()) {
                tools.putIfAbsent(tool.name(), tool);
            }
        }
        return onListPublicEnabledTools(List.copyOf(tools.values()));
    }

    @Override
    public List<McpFunctionDescriptor> listPublicEnabledFunctions() {
        Map<String, McpFunctionDescriptor> functions = new LinkedHashMap<>();
        for (ToolRegistry delegate : delegates) {
            for (McpFunctionDescriptor function : delegate.listPublicEnabledFunctions()) {
                functions.putIfAbsent(function.name(), function);
            }
        }
        return onListPublicEnabledFunctions(List.copyOf(functions.values()));
    }

    @Override
    public List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate) {
        Map<String, McpToolDescriptor> tools = new LinkedHashMap<>();
        for (ToolRegistry delegate : delegates) {
            for (McpToolDescriptor tool : delegate.listRoleVisibleTools(includeDisabled, includePrivate)) {
                tools.putIfAbsent(tool.name(), tool);
            }
        }
        return onListRoleVisibleTools(List.copyOf(tools.values()), includeDisabled, includePrivate);
    }

    @Override
    public Optional<McpToolDescriptor> findEnabledTool(String name) {
        Optional<McpToolDescriptor> tool = delegates.stream()
                .map(delegate -> delegate.findEnabledTool(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        return onFindEnabledTool(tool, name);
    }

    @Override
    public Optional<McpFunctionDescriptor> findEnabledFunction(String name) {
        Optional<McpFunctionDescriptor> function = delegates.stream()
                .map(delegate -> delegate.findEnabledFunction(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        return onFindEnabledFunction(function, name);
    }

    @Override
    public Optional<McpToolDescriptor> findOwningTool(String functionName) {
        Optional<McpToolDescriptor> tool = delegates.stream()
                .map(delegate -> delegate.findOwningTool(functionName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        return onFindOwningTool(tool, functionName);
    }

    @Override
    public Optional<McpToolDescriptor> findTool(String name) {
        Optional<McpToolDescriptor> tool = delegates.stream()
                .map(delegate -> delegate.findTool(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        return onFindTool(tool, name);
    }

    @Override
    public Optional<McpToolHandler> findHandler(String handlerKey) {
        Optional<McpToolHandler> handler = delegates.stream()
                .map(delegate -> delegate.findHandler(handlerKey))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        return onFindHandler(handler, handlerKey);
    }

    @Override
    public ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode) {
        ToolCheckResult result = primary().check(descriptor, updateMode);
        return onCheck(result, descriptor, updateMode);
    }

    @Override
    public McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context) {
        McpToolDescriptor registered = primary().register(descriptor, context);
        return onRegister(registered, descriptor, context);
    }

    @Override
    public McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner) {
        McpToolDescriptor registered = primary().registerRuntimeHandler(handler, owner);
        return onRegisterRuntimeHandler(registered, handler, owner);
    }

    @Override
    public void unregisterRuntimeOwner(String owner) {
        primary().unregisterRuntimeOwner(owner);
        onUnregisterRuntimeOwner(owner);
    }

    @Override
    public McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context) {
        McpToolDescriptor updated = primary().update(name, patch, context);
        return onUpdate(updated, name, patch, context);
    }

    @Override
    public McpToolDescriptor disable(String name, McpCallContext context) {
        McpToolDescriptor disabled = primary().disable(name, context);
        return onDisable(disabled, name, context);
    }

    @Override
    public long registryVersion() {
        long version = delegates.stream()
                .mapToLong(ToolRegistry::registryVersion)
                .max()
                .orElse(0);
        return onRegistryVersion(version);
    }

    @Override
    public List<ToolAuditEvent> auditEvents() {
        List<ToolAuditEvent> events = new ArrayList<>();
        for (ToolRegistry delegate : delegates) {
            events.addAll(delegate.auditEvents());
        }
        return onAuditEvents(events);
    }

    /**
     * Hook after listing public tools.
     */
    protected List<McpToolDescriptor> onListPublicEnabledTools(List<McpToolDescriptor> tools) {
        return tools;
    }

    /**
     * Hook after listing public functions.
     */
    protected List<McpFunctionDescriptor> onListPublicEnabledFunctions(List<McpFunctionDescriptor> functions) {
        return functions;
    }

    /**
     * Hook after listing role-visible tools.
     */
    protected List<McpToolDescriptor> onListRoleVisibleTools(
            List<McpToolDescriptor> tools,
            boolean includeDisabled,
            boolean includePrivate
    ) {
        return tools;
    }

    /**
     * Hook after finding an enabled tool.
     */
    protected Optional<McpToolDescriptor> onFindEnabledTool(Optional<McpToolDescriptor> tool, String name) {
        return tool;
    }

    /**
     * Hook after finding an enabled function.
     */
    protected Optional<McpFunctionDescriptor> onFindEnabledFunction(Optional<McpFunctionDescriptor> function, String name) {
        return function;
    }

    /**
     * Hook after finding a function's owning tool.
     */
    protected Optional<McpToolDescriptor> onFindOwningTool(Optional<McpToolDescriptor> tool, String functionName) {
        return tool;
    }

    /**
     * Hook after finding a tool.
     */
    protected Optional<McpToolDescriptor> onFindTool(Optional<McpToolDescriptor> tool, String name) {
        return tool;
    }

    /**
     * Hook after finding a handler.
     */
    protected Optional<McpToolHandler> onFindHandler(Optional<McpToolHandler> handler, String handlerKey) {
        return handler;
    }

    /**
     * Hook after validating a descriptor.
     */
    protected ToolCheckResult onCheck(ToolCheckResult result, McpToolDescriptor descriptor, boolean updateMode) {
        return result;
    }

    /**
     * Hook after registering a descriptor.
     */
    protected McpToolDescriptor onRegister(McpToolDescriptor registered, McpToolDescriptor descriptor, McpCallContext context) {
        return registered;
    }

    /**
     * Hook after registering a runtime handler.
     */
    protected McpToolDescriptor onRegisterRuntimeHandler(McpToolDescriptor registered, McpToolHandler handler, String owner) {
        return registered;
    }

    /**
     * Hook after unregistering a runtime owner.
     */
    protected void onUnregisterRuntimeOwner(String owner) {
    }

    /**
     * Hook after updating a descriptor.
     */
    protected McpToolDescriptor onUpdate(
            McpToolDescriptor updated,
            String name,
            McpToolPatch patch,
            McpCallContext context
    ) {
        return updated;
    }

    /**
     * Hook after disabling a tool.
     */
    protected McpToolDescriptor onDisable(McpToolDescriptor disabled, String name, McpCallContext context) {
        return disabled;
    }

    /**
     * Hook after reading the registry version.
     */
    protected long onRegistryVersion(long version) {
        return version;
    }

    /**
     * Hook after reading audit events.
     */
    protected List<ToolAuditEvent> onAuditEvents(List<ToolAuditEvent> events) {
        return events;
    }
}
