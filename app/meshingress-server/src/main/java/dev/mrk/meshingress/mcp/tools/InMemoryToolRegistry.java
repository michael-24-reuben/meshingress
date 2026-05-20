package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.tools.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.annotation.AnnotatedMcpToolHandlerProvider;
import dev.mrk.meshingress.api.McpCallContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class InMemoryToolRegistry implements ToolRegistry {

    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9_]*)+$");

    private final ObjectMapper objectMapper;
    private final Map<String, McpToolDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, McpToolHandler> handlers = new LinkedHashMap<>();
    private final List<ToolAuditEvent> auditEvents = new ArrayList<>();
    private long registryVersion = 1;

    public InMemoryToolRegistry(
            ObjectMapper objectMapper,
            List<McpToolHandler> toolHandlers,
            AnnotatedMcpToolHandlerProvider annotatedToolHandlerProvider
    ) {
        this.objectMapper = objectMapper;
        for (McpToolHandler handler : toolHandlers) {
            registerHandler(handler);
        }
        for (McpToolHandler handler : annotatedToolHandlerProvider.handlers()) {
            registerHandler(handler);
        }
    }

    private void registerHandler(McpToolHandler handler) {
        McpToolDescriptor descriptor = handler.descriptor();
        if (descriptors.containsKey(descriptor.name())) {
            throw new IllegalStateException("Duplicate MCP tool descriptor name: " + descriptor.name());
        }
        if (handlers.containsKey(descriptor.handlerKey())) {
            throw new IllegalStateException("Duplicate MCP tool handler key: " + descriptor.handlerKey());
        }
        descriptors.put(descriptor.name(), descriptor);
        handlers.put(descriptor.handlerKey(), handler);
    }

    @Override
    public synchronized List<McpToolDescriptor> listPublicEnabledTools() {
        return descriptors.values().stream()
                .filter(McpToolDescriptor::enabled)
                .filter(descriptor -> descriptor.visibility() == ToolVisibility.PUBLIC)
                .sorted(Comparator.comparing(McpToolDescriptor::name))
                .toList();
    }

    @Override
    public synchronized List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate) {
        return descriptors.values().stream()
                .filter(descriptor -> includeDisabled || descriptor.enabled())
                .filter(descriptor -> includePrivate || descriptor.visibility() == ToolVisibility.PUBLIC)
                .sorted(Comparator.comparing(McpToolDescriptor::name))
                .toList();
    }

    @Override
    public synchronized Optional<McpToolDescriptor> findEnabledTool(String name) {
        return Optional.ofNullable(descriptors.get(name))
                .filter(McpToolDescriptor::enabled)
                .filter(descriptor -> descriptor.visibility() == ToolVisibility.PUBLIC);
    }

    @Override
    public synchronized Optional<McpToolDescriptor> findTool(String name) {
        return Optional.ofNullable(descriptors.get(name));
    }

    @Override
    public synchronized Optional<McpToolHandler> findHandler(String handlerKey) {
        return Optional.ofNullable(handlers.get(handlerKey));
    }

    @Override
    public synchronized ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (descriptor.name() == null || !TOOL_NAME_PATTERN.matcher(descriptor.name()).matches()) {
            errors.add("Tool name must use dotted lower-case segments.");
        }
        if (!updateMode && descriptors.containsKey(descriptor.name())) {
            errors.add("Tool name is already registered.");
        }
        if (descriptor.inputSchema() == null || !descriptor.inputSchema().isObject()) {
            errors.add("inputSchema must be an object.");
        } else if (!"object".equals(descriptor.inputSchema().path("type").asString())) {
            warnings.add("MCP tool inputSchema should normally use type: object.");
        }
        if (descriptor.outputSchema() != null && !descriptor.outputSchema().isObject()) {
            errors.add("outputSchema must be an object when present.");
        }
        if (descriptor.handlerKey() == null || descriptor.handlerKey().isBlank()) {
            errors.add("handlerKey is required.");
        } else if (!handlers.containsKey(descriptor.handlerKey())) {
            errors.add("handlerKey is not allowed or no handler is registered for it.");
        }

        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("name", descriptor.name());
        normalized.put("visibility", descriptor.visibility().toWire());
        normalized.put("enabled", descriptor.enabled());
        normalized.put("inputSchemaDraft", "2020-12");
        return new ToolCheckResult(errors.isEmpty(), errors, warnings, normalized);
    }

    @Override
    public synchronized McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context) {
        ToolCheckResult check = check(descriptor, false);
        if (!check.valid()) {
            throw invalidDescriptor(check);
        }

        McpToolDescriptor registered = descriptor.withVersion(1);
        descriptors.put(registered.name(), registered);
        registryVersion++;
        audit("register", registered.name(), 0, registered.version(), context);
        return registered;
    }

    @Override
    public synchronized McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context) {
        McpToolDescriptor current = descriptors.get(name);
        if (current == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool is not registered.");
        }
        McpToolDescriptor updated = current.withPatch(patch, current.version() + 1);
        ToolCheckResult check = check(updated, true);
        if (!check.valid()) {
            throw invalidDescriptor(check);
        }

        descriptors.put(name, updated);
        registryVersion++;
        audit("update", name, current.version(), updated.version(), context);
        return updated;
    }

    @Override
    public synchronized McpToolDescriptor disable(String name, McpCallContext context) {
        McpToolDescriptor current = descriptors.get(name);
        if (current == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool is not registered.");
        }
        McpToolPatch patch = new McpToolPatch(null, null, false, null, null, null, null, null);
        McpToolDescriptor updated = current.withPatch(patch, current.version() + 1);
        descriptors.put(name, updated);
        registryVersion++;
        audit("disable", name, current.version(), updated.version(), context);
        return updated;
    }

    @Override
    public synchronized long registryVersion() {
        return registryVersion;
    }

    @Override
    public synchronized List<ToolAuditEvent> auditEvents() {
        return List.copyOf(auditEvents);
    }

    private JsonRpcException invalidDescriptor(ToolCheckResult check) {
        ObjectNode data = objectMapper.createObjectNode();
        ArrayNode errors = objectMapper.createArrayNode();
        check.errors().forEach(errors::add);
        data.set("errors", errors);
        return new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool descriptor is invalid.", data);
    }

    private void audit(String action, String toolName, int previousVersion, int newVersion, McpCallContext context) {
        auditEvents.add(new ToolAuditEvent(
                OffsetDateTime.now(),
                context.authorizationHeader() == null ? "role-header" : "bearer-role-admin",
                action,
                toolName,
                previousVersion,
                newVersion,
                registryVersion,
                context.requestId()
        ));
    }
}
