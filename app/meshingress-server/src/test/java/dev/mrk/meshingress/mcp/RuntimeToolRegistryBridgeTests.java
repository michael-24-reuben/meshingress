package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionActivationMode;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionConflict;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionStore;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeToolRegistryBridgeTests {

    @Test
    void extensionWaitsForPrimaryThenBecomesFallbackAfterPrimaryIsRemoved() {
        RecordingContributionStore store = new RecordingContributionStore();
        ToolModuleId officialId = new ToolModuleId("official-module");
        ToolModuleId extensionId = new ToolModuleId("extension-module");
        store.reserve(contribution("official", 10, ToolContributionActivationMode.PRIMARY, officialId.value()));
        store.reserve(contribution("extension", 20, ToolContributionActivationMode.REQUIRES_PRIMARY, extensionId.value()));
        RecordingToolRegistry registry = new RecordingToolRegistry();
        RuntimeToolRegistryBridge bridge = new RuntimeToolRegistryBridge(registry, store);

        bridge.register(extensionId, List.of(handler("extension", "youtube.video.metadata", "youtube.video.download")));
        assertThat(registry.functions).isEmpty();
        assertThat(store.byToolId("extension").status()).isEqualTo("pending-primary");

        bridge.register(officialId, List.of(handler("official", "youtube.video.metadata")));
        assertThat(registry.functions.keySet()).containsExactlyInAnyOrder("youtube.video.metadata", "youtube.video.download");
        assertThat(registry.owners.get("youtube.video.metadata")).isEqualTo(officialId.value());
        assertThat(registry.owners.get("youtube.video.download")).isEqualTo(extensionId.value());
        assertThat(store.conflicts).extracting(ToolContributionConflict::functionName).containsExactly("youtube.video.metadata");

        ToolContributionRecord extensionFallback = contribution("extension", 20,
                ToolContributionActivationMode.CONTRIBUTOR_WITH_FALLBACK, extensionId.value());
        store.reserve(extensionFallback);
        bridge.refreshNamespace("youtube");
        bridge.unregister(officialId);

        assertThat(registry.functions.keySet()).containsExactlyInAnyOrder("youtube.video.metadata", "youtube.video.download");
        assertThat(registry.owners.get("youtube.video.metadata")).isEqualTo(extensionId.value());
        assertThat(store.byToolId("extension").status()).isEqualTo("active");
    }

    private ToolContributionRecord contribution(String toolId, int precedence, ToolContributionActivationMode mode, String runtimeModuleId) {
        return new ToolContributionRecord(toolId, "youtube", precedence, mode, "reserved",
                OffsetDateTime.parse("2026-08-07T00:00:00Z"), runtimeModuleId);
    }

    private McpToolHandler handler(String owner, String... names) {
        List<McpFunctionDescriptor> functions = java.util.Arrays.stream(names)
                .map(name -> new McpFunctionDescriptor(name, name, "", 1, true, ToolVisibility.PUBLIC,
                        owner + ":" + name, null, null, null, true))
                .toList();
        return new McpToolHandler() {
            @Override public McpToolDescriptor descriptor() {
                return new McpToolDescriptor("youtube", "YouTube", "", 1, true, ToolVisibility.PUBLIC, functions, null, true);
            }
            @Override public DispatchExecutionResult call(ObjectNode arguments, McpCallContext context) { return DispatchExecutionResult.create(); }
        };
    }

    private static final class RecordingContributionStore implements ToolContributionStore {
        private final Map<String, ToolContributionRecord> records = new LinkedHashMap<>();
        private List<ToolContributionConflict> conflicts = List.of();
        @Override public ToolContributionRecord reserve(ToolContributionRecord contribution) {
            records.put(contribution.namespace() + ":" + contribution.toolId(), contribution);
            return contribution;
        }
        @Override public List<ToolContributionRecord> listNamespace(String namespace) {
            return records.values().stream().filter(value -> value.namespace().equals(namespace))
                    .sorted(java.util.Comparator.comparingInt(ToolContributionRecord::precedence)).toList();
        }
        @Override public List<ToolContributionRecord> listContributions() { return List.copyOf(records.values()); }
        @Override public void replaceConflicts(String namespace, List<ToolContributionConflict> conflicts) { this.conflicts = List.copyOf(conflicts); }
        @Override public List<ToolContributionConflict> listConflicts(String namespace) { return conflicts; }
        ToolContributionRecord byToolId(String toolId) { return records.values().stream().filter(value -> value.toolId().equals(toolId)).findFirst().orElseThrow(); }
    }

    private static final class RecordingToolRegistry implements ToolRegistry {
        private final Map<String, McpFunctionDescriptor> functions = new LinkedHashMap<>();
        private final Map<String, String> owners = new LinkedHashMap<>();
        private final Map<String, McpToolHandler> handlers = new LinkedHashMap<>();
        @Override public McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner) {
            for (McpFunctionDescriptor function : handler.descriptor().functions()) {
                functions.putIfAbsent(function.name(), function);
                if (functions.get(function.name()) == function) { owners.put(function.name(), owner); handlers.put(function.handlerKey(), handler); }
            }
            return handler.descriptor();
        }
        @Override public void unregisterRuntimeOwner(String owner) {
            owners.entrySet().removeIf(entry -> { if (!entry.getValue().equals(owner)) return false; functions.remove(entry.getKey()); return true; });
            handlers.entrySet().removeIf(entry -> owners.values().stream().noneMatch(value -> value.equals(owner)));
        }
        @Override public Optional<McpToolHandler> findHandler(String key) { return Optional.ofNullable(handlers.get(key)); }
        @Override public List<McpToolDescriptor> listPublicEnabledTools() { return List.of(); }
        @Override public List<McpFunctionDescriptor> listPublicEnabledFunctions() { return List.copyOf(functions.values()); }
        @Override public List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate) { return List.of(); }
        @Override public Optional<McpToolDescriptor> findEnabledTool(String name) { return Optional.empty(); }
        @Override public Optional<McpFunctionDescriptor> findEnabledFunction(String name) { return Optional.ofNullable(functions.get(name)); }
        @Override public Optional<McpToolDescriptor> findOwningTool(String functionName) { return Optional.empty(); }
        @Override public Optional<McpToolDescriptor> findTool(String name) { return Optional.empty(); }
        @Override public ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode) { return null; }
        @Override public McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context) { return descriptor; }
        @Override public McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context) { return null; }
        @Override public McpToolDescriptor disable(String name, McpCallContext context) { return null; }
        @Override public long registryVersion() { return 0; }
        @Override public List<ToolAuditEvent> auditEvents() { return List.of(); }
    }
}
