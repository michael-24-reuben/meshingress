package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionActivationMode;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionResolver;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionStore;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.registry.ToolModuleRegistration;
import dev.mrk.meshingress.runtime.registry.ToolRegistrationBridge;
import dev.mrk.meshingress.toolcatalog.ToolModuleCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registers dynamic modules by persisted family precedence rather than discovery order. */
@Component
public final class RuntimeToolRegistryBridge implements ToolRegistrationBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeToolRegistryBridge.class);
    private final ToolRegistry toolRegistry;
    private final ToolContributionStore contributions;
    private final ToolModuleCatalog moduleCatalog;
    private final Map<String, Candidate> candidates = new LinkedHashMap<>();
    private final Map<String, ToolModuleRegistration> registrations = new LinkedHashMap<>();

    public RuntimeToolRegistryBridge(ToolRegistry toolRegistry, ToolContributionStore contributions) {
        this(toolRegistry, contributions, null);
    }

    @Autowired
    public RuntimeToolRegistryBridge(ToolRegistry toolRegistry, ToolContributionStore contributions, ToolModuleCatalog moduleCatalog) {
        this.toolRegistry = toolRegistry;
        this.contributions = contributions;
        this.moduleCatalog = moduleCatalog;
    }

    @Override
    public synchronized ToolModuleRegistration register(ToolModuleId moduleId, List<McpToolHandler> handlers) {
        String namespace = namespace(handlers);
        ToolContributionRecord contribution = contributions.listNamespace(namespace).stream()
                .filter(value -> moduleId.value().equals(value.runtimeModuleId()))
                .findFirst()
                .orElseGet(() -> contributions.reserve(new ToolContributionRecord(moduleId.value(), namespace,
                        nextPrecedence(namespace), ToolContributionActivationMode.CONTRIBUTOR, "reserved", null, moduleId.value())));
        candidates.put(moduleId.value(), new Candidate(moduleId, namespace, contribution, List.copyOf(handlers)));
        rebuild(namespace);
        return registrations.getOrDefault(moduleId.value(), new ToolModuleRegistration(List.of()));
    }

    @Override
    public synchronized void unregister(ToolModuleId moduleId) {
        Candidate removed = candidates.remove(moduleId.value());
        toolRegistry.unregisterRuntimeOwner(moduleId.value());
        registrations.remove(moduleId.value());
        if (moduleCatalog != null) moduleCatalog.unregister(moduleId.value());
        if (removed != null) rebuild(removed.namespace());
    }

    /** Re-applies persisted administrator precedence to the currently loaded namespace. */
    public synchronized void refreshNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) return;
        String normalizedNamespace = namespace.strip();
        List<ToolContributionRecord> persisted = contributions.listNamespace(normalizedNamespace);
        candidates.entrySet().stream()
                .filter(entry -> entry.getValue().namespace().equals(normalizedNamespace))
                .toList()
                .forEach(entry -> persisted.stream()
                        .filter(value -> value.toolId().equals(entry.getValue().contribution().toolId()))
                        .findFirst()
                        .ifPresent(value -> candidates.put(entry.getKey(), new Candidate(
                                entry.getValue().moduleId(), normalizedNamespace, value, entry.getValue().handlers()))));
        rebuild(normalizedNamespace);
    }

    private void rebuild(String namespace) {
        List<Candidate> family = candidates.values().stream().filter(candidate -> candidate.namespace().equals(namespace))
                .sorted(Comparator.comparingInt(candidate -> candidate.contribution().precedence())).toList();
        family.forEach(candidate -> toolRegistry.unregisterRuntimeOwner(candidate.moduleId().value()));
        boolean primaryPresent = family.stream().anyMatch(candidate -> candidate.contribution().activationMode() == ToolContributionActivationMode.PRIMARY);
        for (Candidate candidate : family) {
            if (candidate.contribution().activationMode() == ToolContributionActivationMode.REQUIRES_PRIMARY && !primaryPresent) {
                registrations.put(candidate.moduleId().value(), new ToolModuleRegistration(List.of()));
                contributions.reserve(candidate.contribution().withStatus("pending-primary"));
                continue;
            }
            List<String> registered = new ArrayList<>();
            for (McpToolHandler handler : candidate.handlers()) {
                toolRegistry.registerRuntimeHandler(handler, candidate.moduleId().value());
                for (McpFunctionDescriptor function : handler.descriptor().functions()) {
                    if (toolRegistry.findHandler(function.handlerKey()).orElse(null) == handler) registered.add(function.name());
                }
            }
            registrations.put(candidate.moduleId().value(), new ToolModuleRegistration(List.copyOf(registered)));
            String state = registered.isEmpty() ? "pending" : registered.size() == candidate.functions().size() ? "active" : "partially-active";
            contributions.reserve(candidate.contribution().withStatus(state));
        }
        List<Candidate> eligible = family.stream()
                .filter(candidate -> candidate.contribution().activationMode() != ToolContributionActivationMode.REQUIRES_PRIMARY || primaryPresent)
                .toList();
        List<ToolContributionResolver.ContributionFunctions> functions = eligible.stream()
                .map(candidate -> new ToolContributionResolver.ContributionFunctions(candidate.contribution(), candidate.functions())).toList();
        ToolContributionResolver.Resolution resolution = new ToolContributionResolver().resolve(namespace, functions);
        contributions.replaceConflicts(namespace, resolution.conflicts().stream()
                .map(conflict -> new dev.mrk.meshingress.controller.roles.registration.ToolContributionConflict(
                        namespace, conflict.functionName(), conflict.owningToolId(), conflict.rejectedToolId(), null))
                .toList());
        resolution.conflicts().forEach(conflict ->
                LOGGER.warn("Tool contribution conflict namespace={} function={} ownerToolId={} rejectedToolId={}",
                        namespace, conflict.functionName(), conflict.owningToolId(), conflict.rejectedToolId()));
    }

    private int nextPrecedence(String namespace) {
        return contributions.listNamespace(namespace).stream().mapToInt(ToolContributionRecord::precedence).max().orElse(-1) + 1;
    }

    private String namespace(List<McpToolHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) throw new IllegalArgumentException("Runtime module must expose at least one handler");
        String namespace = null;
        for (McpToolHandler handler : handlers) for (McpFunctionDescriptor function : handler.descriptor().functions()) {
            String candidate = function.name().split("\\.", 2)[0];
            if (namespace == null) namespace = candidate;
            else if (!namespace.equals(candidate)) throw new IllegalArgumentException("Runtime module functions must share one namespace");
        }
        if (namespace == null) throw new IllegalArgumentException("Runtime module has no functions");
        return namespace;
    }

    private record Candidate(ToolModuleId moduleId, String namespace, ToolContributionRecord contribution, List<McpToolHandler> handlers) {
        List<McpFunctionDescriptor> functions() { return handlers.stream().flatMap(handler -> handler.descriptor().functions().stream()).toList(); }
    }
}
