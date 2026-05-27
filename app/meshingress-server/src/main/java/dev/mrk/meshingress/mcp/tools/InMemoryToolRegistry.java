package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.tools.*;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
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

@Service
public class InMemoryToolRegistry implements ToolRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryToolRegistry.class);
    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");
    private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");

    private final ObjectMapper objectMapper;
    private final MeshingressProperties properties;
    private final Map<String, McpToolDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, McpFunctionDescriptor> functions = new LinkedHashMap<>();
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
            handlers.put(function.handlerKey(), handler);
            registeredFunctionNames.add(function.name());
        }
        if (runtimeOwner != null && !registeredFunctionNames.isEmpty()) {
            runtimeOwnerFunctions.computeIfAbsent(runtimeOwner, ignored -> new ArrayList<>()).addAll(registeredFunctionNames);
            runtimeOwnerTools.computeIfAbsent(runtimeOwner, ignored -> new ArrayList<>()).add(descriptor.name());
        }
    }

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
        if (!registryEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(descriptors.get(name))
                .filter(McpToolDescriptor::enabled)
                .filter(this::toolAllowedForCall);
    }

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
        return checkDescriptorShape(descriptor, updateMode, true);
    }

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
        }
        registryVersion++;
        audit("register", registered.name(), 0, registered.version(), context);
        return registered;
    }

    @Override
    public synchronized McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner) {
        if (owner == null || owner.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Runtime tool owner is required.");
        }
        registerHandler(handler, owner);
        registryVersion++;
        return handler.descriptor();
    }

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
        }
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

    private boolean registryEnabled() {
        return properties.tools().registry().enabled();
    }

    private boolean toolVisibleToPublic(McpToolDescriptor descriptor) {
        return (properties.tools().registry().includeDisabled() || descriptor.enabled())
                && visibilityAllowed(descriptor.visibility())
                && allowedByLists(descriptor.name());
    }

    private boolean functionVisibleToPublic(McpFunctionDescriptor function) {
        return (properties.tools().registry().includeDisabled() || function.enabled())
                && visibilityAllowed(function.visibility())
                && allowedByLists(function.name());
    }

    private boolean toolAllowedForCall(McpToolDescriptor descriptor) {
        return visibilityAllowed(descriptor.visibility()) && allowedByLists(descriptor.name());
    }

    private boolean functionAllowedForCall(McpFunctionDescriptor function) {
        return visibilityAllowed(function.visibility()) && allowedByLists(function.name());
    }

    private boolean visibilityAllowed(ToolVisibility visibility) {
        return visibility == ToolVisibility.PUBLIC || properties.tools().registry().exposePrivateTools();
    }

    private boolean allowedByLists(String name) {
        List<String> allowList = properties.tools().allowList();
        List<String> denyList = properties.tools().denyList();
        return (allowList.isEmpty() || allowList.contains(name)) && !denyList.contains(name);
    }

    private void handleInvalidStartupDescriptor(McpToolDescriptor descriptor, ToolCheckResult check) {
        String message = "Invalid MCP tool descriptor %s: %s".formatted(descriptor.name(), String.join("; ", check.errors()));
        if (properties.tools().registry().failOnInvalidToolId()) {
            throw new IllegalStateException(message);
        }
        LOGGER.warn("{}; skipping because meshingress.tools.registry.fail-on-invalid-tool-id=false", message);
    }

    private void handleDuplicate(String message) {
        if (properties.tools().registry().failOnDuplicateToolId()) {
            throw new IllegalStateException(message);
        }
        LOGGER.warn("{}; keeping first registration because meshingress.tools.registry.fail-on-duplicate-tool-id=false", message);
    }

    private Optional<McpToolDescriptor> owningTool(String functionName) {
        return descriptors.values().stream()
                .filter(descriptor -> descriptor.functions().stream()
                        .anyMatch(function -> function.name().equals(functionName)))
                .findFirst();
    }

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
