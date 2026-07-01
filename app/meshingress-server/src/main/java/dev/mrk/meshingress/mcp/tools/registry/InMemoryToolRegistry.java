package dev.mrk.meshingress.mcp.tools.registry;

import dev.mrk.meshingress.api.tools.*;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.annotation.AnnotatedMcpToolHandlerProvider;
import dev.mrk.meshingress.api.McpCallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * In-memory registry for MCP tool descriptors, functions, and handlers.
 *
 * <p>Tracks visibility, enablement, runtime ownership, and audit events while
 * enforcing naming and schema constraints for registered tools.</p>
 */
@Service
public class InMemoryToolRegistry implements ToolRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryToolRegistry.class);
    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-_]*(\\.[a-z][a-z0-9-_]*)*$");
    private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-_]*(\\.[a-z][a-z0-9-_]*)+$");

    private final ObjectMapper objectMapper;
    private final MeshingressProperties properties;
    private final Map<String, McpToolDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, McpFunctionDescriptor> functions = new LinkedHashMap<>();
    private final Map<String, String> functionOwners = new LinkedHashMap<>();
    private final Map<String, McpToolHandler> handlers = new LinkedHashMap<>();
    private final Map<String, List<String>> runtimeOwnerFunctions = new LinkedHashMap<>();
    private final Map<String, List<String>> runtimeOwnerTools = new LinkedHashMap<>();
    private final List<ToolAuditEvent> auditEvents = new ArrayList<>();
    private long registryVersion = 1;

    public InMemoryToolRegistry(
            ObjectMapper objectMapper,
            MeshingressProperties properties,
            List<McpToolHandler> toolHandlers,
            AnnotatedMcpToolHandlerProvider annotatedToolHandlerProvider
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        if (properties.tools().registry().scanOnStartup()) {
            for (McpToolHandler handler : toolHandlers) {
                registerHandler(handler, null);
            }
            for (McpToolHandler handler : annotatedToolHandlerProvider.handlers()) {
                registerHandler(handler, null);
            }
        } else {
            LOGGER.info("MCP tool registry startup scan disabled by meshingress.tools.registry.scan-on-startup=false");
        }
    }

    /**
     * Register a handler and merge its descriptor into the registry.
     */
    private void registerHandler(McpToolHandler handler, String runtimeOwner) {
        McpToolDescriptor descriptor = handler.descriptor();
        ToolCheckResult check = checkDescriptorShape(descriptor, true, false);
        if (!check.valid()) {
            handleInvalidStartupDescriptor(descriptor, check);
            return;
        }
        if (descriptor.functions().isEmpty()) {
            throw new IllegalStateException("MCP tool descriptor must expose at least one function: " + descriptor.name());
        }
        McpToolDescriptor existing = descriptors.get(descriptor.name());
        if (existing == null) {
            descriptors.put(descriptor.name(), descriptor);
        } else {
            List<McpFunctionDescriptor> mergedFunctions = new ArrayList<>(existing.functions());
            mergedFunctions.addAll(descriptor.functions());
            descriptors.put(descriptor.name(), existing.withFunctions(mergedFunctions));
        }
        List<String> registeredFunctionNames = new ArrayList<>();
        for (McpFunctionDescriptor function : descriptor.functions()) {
            if (functions.containsKey(function.name())) {
                handleDuplicate("Duplicate MCP function descriptor name: " + function.name());
                continue;
            }
            if (handlers.containsKey(function.handlerKey())) {
                handleDuplicate("Duplicate MCP function handler key: " + function.handlerKey());
                continue;
            }
            functions.put(function.name(), function);
            functionOwners.put(function.name(), descriptor.name());
            handlers.put(function.handlerKey(), handler);
            registeredFunctionNames.add(function.name());
        }
        if (runtimeOwner != null && !registeredFunctionNames.isEmpty()) {
            runtimeOwnerFunctions.computeIfAbsent(runtimeOwner, ignored -> new ArrayList<>()).addAll(registeredFunctionNames);
            runtimeOwnerTools.computeIfAbsent(runtimeOwner, ignored -> new ArrayList<>()).add(descriptor.name());
        }
    }

    /**
     * List public-facing enabled tools, honoring visibility and allow/deny lists.
     */
    @Override
    public synchronized List<McpToolDescriptor> listPublicEnabledTools() {
        if (!registryEnabled()) {
            return List.of();
        }
        return descriptors.values().stream()
                .filter(this::toolVisibleToPublic)
                .sorted(Comparator.comparing(McpToolDescriptor::name))
                .toList();
    }

    /**
     * List public-facing enabled functions, honoring visibility and allow/deny lists.
     */
    @Override
    public synchronized List<McpFunctionDescriptor> listPublicEnabledFunctions() {
        if (!registryEnabled()) {
            return List.of();
        }
        return descriptors.values().stream()
                .filter(this::toolVisibleToPublic)
                .flatMap(descriptor -> descriptor.functions().stream())
                .filter(this::functionVisibleToPublic)
                .sorted(Comparator.comparing(McpFunctionDescriptor::name))
                .toList();
    }

    /**
     * List tools visible to role-gated callers with optional disabled/private inclusion.
     */
    @Override
    public synchronized List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate) {
        return descriptors.values().stream()
                .filter(descriptor -> includeDisabled || descriptor.enabled())
                .filter(descriptor -> includePrivate || descriptor.visibility() == ToolVisibility.PUBLIC)
                .sorted(Comparator.comparing(McpToolDescriptor::name))
                .toList();
    }

    /**
     * Find a tool that is enabled and allowed for external calls.
     */
    @Override
    public synchronized Optional<McpToolDescriptor> findEnabledTool(String name) {
        if (!registryEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(descriptors.get(name))
                .filter(McpToolDescriptor::enabled)
                .filter(this::toolAllowedForCall);
    }

    /**
     * Find a function that is enabled, allowed, and whose owning tool is callable.
     */
    @Override
    public synchronized Optional<McpFunctionDescriptor> findEnabledFunction(String name) {
        if (!registryEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(functions.get(name))
                .filter(McpFunctionDescriptor::enabled)
                .filter(this::functionAllowedForCall)
                .filter(function -> owningTool(function.name())
                        .map(descriptor -> descriptor.enabled() && toolAllowedForCall(descriptor))
                        .orElse(false));
    }

    /**
     * Find the registered tool descriptor that owns a function name.
     */
    @Override
    public synchronized Optional<McpToolDescriptor> findOwningTool(String functionName) {
        return owningTool(functionName);
    }

    /**
     * Find a tool regardless of enablement or visibility.
     */
    @Override
    public synchronized Optional<McpToolDescriptor> findTool(String name) {
        return Optional.ofNullable(descriptors.get(name));
    }

    /**
     * Find a handler by its handler key.
     */
    @Override
    public synchronized Optional<McpToolHandler> findHandler(String handlerKey) {
        return Optional.ofNullable(handlers.get(handlerKey));
    }

    /**
     * Validate a descriptor and return normalized JSON plus errors/warnings.
     */
    @Override
    public synchronized ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode) {
        return checkDescriptorShape(descriptor, updateMode, true);
    }

    /**
     * Build validation results and normalized JSON for a descriptor.
     */
    private ToolCheckResult checkDescriptorShape(McpToolDescriptor descriptor, boolean updateMode, boolean validateHandlerKey) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (descriptor.name() == null || !TOOL_NAME_PATTERN.matcher(descriptor.name()).matches()) {
            errors.add("Tool name must use lower-case dotted segments.");
        }
        if (!updateMode && descriptors.containsKey(descriptor.name())) {
            errors.add("Tool name is already registered.");
        }
        if (descriptor.functions().isEmpty()) {
            errors.add("At least one function descriptor is required.");
        }
        for (McpFunctionDescriptor function : descriptor.functions()) {
            checkFunction(function, errors, warnings, validateHandlerKey);
        }

        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("name", descriptor.name());
        normalized.put("visibility", descriptor.visibility().toWire());
        normalized.put("enabled", descriptor.enabled());
        ArrayNode functionNodes = objectMapper.createArrayNode();
        descriptor.functions().forEach(function -> functionNodes.add(function.toMcpJson(objectMapper)));
        normalized.set("functions", functionNodes);
        return new ToolCheckResult(errors.isEmpty(), errors, warnings, normalized);
    }

    /**
     * Register a tool descriptor and initialize versioning and audit trail.
     */
    @Override
    public synchronized McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context) {
        ToolCheckResult check = check(descriptor, false);
        if (!check.valid()) {
            throw invalidDescriptor(check);
        }

        McpToolDescriptor registered = descriptor.withVersion(1);
        descriptors.put(registered.name(), registered);
        for (McpFunctionDescriptor function : registered.functions()) {
            functions.put(function.name(), function.withVersion(registered.version()));
            functionOwners.put(function.name(), registered.name());
        }
        registryVersion++;
        audit("register", registered.name(), 0, registered.version(), context);
        return registered;
    }

    /**
     * Register a runtime handler and associate it with an owner for later cleanup.
     */
    @Override
    public synchronized McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner) {
        if (owner == null || owner.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Runtime tool owner is required.");
        }
        registerHandler(handler, owner);
        registryVersion++;
        return handler.descriptor();
    }

    /**
     * Unregister all runtime tools/functions associated with an owner.
     */
    @Override
    public synchronized void unregisterRuntimeOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Runtime tool owner is required.");
        }
        List<String> ownedFunctions = runtimeOwnerFunctions.remove(owner);
        List<String> ownedTools = runtimeOwnerTools.remove(owner);
        if (ownedFunctions == null || ownedFunctions.isEmpty()) {
            return;
        }

        for (String functionName : ownedFunctions) {
            McpFunctionDescriptor function = functions.remove(functionName);
            functionOwners.remove(functionName);
            if (function != null) {
                handlers.remove(function.handlerKey());
            }
        }
        if (ownedTools != null) {
            for (String toolName : ownedTools) {
                McpToolDescriptor descriptor = descriptors.get(toolName);
                if (descriptor == null) {
                    continue;
                }
                List<McpFunctionDescriptor> remainingFunctions = descriptor.functions().stream()
                        .filter(function -> !ownedFunctions.contains(function.name()))
                        .toList();
                if (remainingFunctions.isEmpty()) {
                    descriptors.remove(toolName);
                } else {
                    descriptors.put(toolName, descriptor.withFunctions(remainingFunctions));
                }
            }
        }
        registryVersion++;
    }

    /**
     * Apply a patch to a tool and its first function and bump versions.
     */
    @Override
    public synchronized McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context) {
        McpToolDescriptor current = descriptors.get(name);
        if (current == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool is not registered.");
        }
        McpToolDescriptor updated = current.withPatch(patch, current.version() + 1)
                .withFunctions(patchedFunctions(current.functions(), patch, current.version() + 1));
        ToolCheckResult check = check(updated, true);
        if (!check.valid()) {
            throw invalidDescriptor(check);
        }

        descriptors.put(name, updated);
        for (McpFunctionDescriptor function : updated.functions()) {
            functions.put(function.name(), function);
            functionOwners.put(function.name(), name);
        }
        registryVersion++;
        audit("update", name, current.version(), updated.version(), context);
        return updated;
    }

    /**
     * Disable a tool and record the change in the audit log.
     */
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

    /**
     * Current registry version used for list change detection.
     */
    @Override
    public synchronized long registryVersion() {
        return registryVersion;
    }

    /**
     * Return a snapshot of audit events.
     */
    @Override
    public synchronized List<ToolAuditEvent> auditEvents() {
        return List.copyOf(auditEvents);
    }

    /**
     * Build a JSON-RPC exception for invalid descriptors.
     */
    private JsonRpcException invalidDescriptor(ToolCheckResult check) {
        ObjectNode data = objectMapper.createObjectNode();
        ArrayNode errors = objectMapper.createArrayNode();
        check.errors().forEach(errors::add);
        data.set("errors", errors);
        return new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool descriptor is invalid.", data);
    }

    /**
     * Validate a single function descriptor and add findings.
     */
    private void checkFunction(McpFunctionDescriptor function, List<String> errors, List<String> warnings, boolean validateHandlerKey) {
        if (function.name() == null || !FUNCTION_NAME_PATTERN.matcher(function.name()).matches()) {
            errors.add("Function name must join tool and function names with a period.");
        }
        if (function.inputSchema() == null || !function.inputSchema().isObject()) {
            errors.add("function inputSchema must be an object.");
        } else if (!"object".equals(function.inputSchema().path("type").asString())) {
            warnings.add("MCP function inputSchema should normally use type: object.");
        }
        if (function.outputSchema() != null && !function.outputSchema().isObject()) {
            errors.add("function outputSchema must be an object when present.");
        }
        if (function.handlerKey() == null || function.handlerKey().isBlank()) {
            errors.add("function handlerKey is required.");
        } else if (validateHandlerKey && !handlers.containsKey(function.handlerKey())) {
            errors.add("function handlerKey is not allowed or no handler is registered for it.");
        }
    }

    /**
     * Whether registry operations are enabled by configuration.
     */
    private boolean registryEnabled() {
        return properties.tools().registry().enabled();
    }

    /**
     * Public visibility rules for tools.
     */
    private boolean toolVisibleToPublic(McpToolDescriptor descriptor) {
        return allowedByPolicy(descriptor.enabled(), descriptor.visibility(), descriptor.name(), properties.tools().registry().includeDisabled());
    }

    /**
     * Public visibility rules for functions.
     */
    private boolean functionVisibleToPublic(McpFunctionDescriptor function) {
        return allowedByPolicy(function.enabled(), function.visibility(), function.name(), properties.tools().registry().includeDisabled());
    }

    /**
     * Call-eligibility rules for tools.
     */
    private boolean toolAllowedForCall(McpToolDescriptor descriptor) {
        return allowedByPolicy(descriptor.enabled(), descriptor.visibility(), descriptor.name(), true);
    }

    /**
     * Call-eligibility rules for functions.
     */
    private boolean functionAllowedForCall(McpFunctionDescriptor function) {
        return allowedByPolicy(function.enabled(), function.visibility(), function.name(), true);
    }

    private boolean allowedByPolicy(boolean enabled, ToolVisibility visibility, String name, boolean includeDisabled) {
        return (includeDisabled || enabled)
                && visibilityAllowed(visibility)
                && allowedByLists(name);
    }

    /**
     * Whether a visibility level is permitted by configuration.
     */
    private boolean visibilityAllowed(ToolVisibility visibility) {
        return visibility == ToolVisibility.PUBLIC || properties.tools().registry().exposePrivateTools();
    }

    /**
     * Apply allow/deny list checks to a tool or function name.
     */
    private boolean allowedByLists(String name) {
        List<String> allowList = properties.tools().allowList();
        List<String> denyList = properties.tools().denyList();
        return (allowList.isEmpty() || allowList.contains(name)) && !denyList.contains(name);
    }

    /**
     * Handle invalid startup descriptors based on configured strictness.
     */
    private void handleInvalidStartupDescriptor(McpToolDescriptor descriptor, ToolCheckResult check) {
        String message = "Invalid MCP tool descriptor %s: %s".formatted(descriptor.name(), String.join("; ", check.errors()));
        if (properties.tools().registry().failOnInvalidToolId()) {
            throw new IllegalStateException(message);
        }
        LOGGER.warn("{}; skipping because meshingress.tools.registry.fail-on-invalid-tool-id=false", message);
    }

    /**
     * Handle duplicate registrations based on configured strictness.
     */
    private void handleDuplicate(String message) {
        if (properties.tools().registry().failOnDuplicateToolId()) {
            throw new IllegalStateException(message);
        }
        LOGGER.warn("{}; keeping first registration because meshingress.tools.registry.fail-on-duplicate-tool-id=false", message);
    }

    /**
     * Find the tool descriptor that owns a given function name.
     */
    private Optional<McpToolDescriptor> owningTool(String functionName) {
        return Optional.ofNullable(functionOwners.get(functionName))
                .map(descriptors::get);
    }

    /**
     * Apply patch fields to the first function descriptor when requested.
     */
    private List<McpFunctionDescriptor> patchedFunctions(List<McpFunctionDescriptor> current, McpToolPatch patch, int nextVersion) {
        boolean patchesFunction = patch.handlerKey() != null
                || patch.inputSchema() != null
                || patch.outputSchema() != null
                || patch.enabled() != null
                || patch.visibility() != null;
        if (!patchesFunction || current.isEmpty()) {
            return current;
        }

        List<McpFunctionDescriptor> next = new ArrayList<>(current);
        next.set(0, current.getFirst().withPatch(patch, nextVersion));
        return List.copyOf(next);
    }

    /**
     * Append an audit event for a registry mutation.
     */
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
