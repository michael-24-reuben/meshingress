package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactChecksum;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactProvenance;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationPhase;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationStore;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleState;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleStatus;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactInstallerRollbackTests {

    @Test
    void installDeactivatesRuntimeModuleWhenRegistrationSaveFails() {
        ToolModuleId moduleId = new ToolModuleId("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        RecordingRuntimeLoader runtimeLoader = new RecordingRuntimeLoader(moduleId, "sample.echo");
        RecordingRuntimeToolCache runtimeToolCache = new RecordingRuntimeToolCache();
        FailingRegistrationStore registrationStore = new FailingRegistrationStore();
        ArtifactInstaller installer = new ArtifactInstaller(
                new NoOpPublicationRecordVerifier(),
                new NoOpInstallPolicyEvaluator(),
                runtimeToolCache,
                runtimeLoader,
                registrationStore,
                new FixedToolRegistry("sample.echo")
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> installer.install(publication(), "sample.echo", new McpCallContext("Bearer dev-admin", null, null, "req-1"))
        );

        assertThat(failure).isSameAs(registrationStore.failure);
        assertThat(runtimeLoader.deactivatedModuleId).isEqualTo(moduleId);
        assertThat(runtimeToolCache.removedPath).isEqualTo(runtimeToolCache.installedPath);
    }

    @Test
    void installDeletesRuntimeCacheWhenActivationFails() {
        RuntimeException activationFailure = new RuntimeException("activation failed");
        FailingActivationRuntimeLoader runtimeLoader = new FailingActivationRuntimeLoader(activationFailure);
        RecordingRuntimeToolCache runtimeToolCache = new RecordingRuntimeToolCache();
        ArtifactInstaller installer = new ArtifactInstaller(
                new NoOpPublicationRecordVerifier(),
                new NoOpInstallPolicyEvaluator(),
                runtimeToolCache,
                runtimeLoader,
                new FailingRegistrationStore(),
                new FixedToolRegistry("sample.echo")
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> installer.install(publication(), "sample.echo", new McpCallContext("Bearer dev-admin", null, null, "req-1"))
        );

        assertThat(failure).isSameAs(activationFailure);
        assertThat(runtimeToolCache.removedPath).isEqualTo(runtimeToolCache.installedPath);
        assertThat(runtimeLoader.deactivatedModuleId).isNull();
    }

    @Test
    void installDeletesRuntimeCacheWhenPartialActivationFailsBeforeHandleReturns() {
        RuntimeException activationFailure = new RuntimeException("partial activation failed");
        PartiallyActivatingRuntimeLoader runtimeLoader = new PartiallyActivatingRuntimeLoader(activationFailure);
        RecordingRuntimeToolCache runtimeToolCache = new RecordingRuntimeToolCache();
        ArtifactInstaller installer = new ArtifactInstaller(
                new NoOpPublicationRecordVerifier(),
                new NoOpInstallPolicyEvaluator(),
                runtimeToolCache,
                runtimeLoader,
                new FailingRegistrationStore(),
                new FixedToolRegistry("sample.echo")
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> installer.install(publication(), "sample.echo", new McpCallContext("Bearer dev-admin", null, null, "req-1"))
        );

        assertThat(failure).isSameAs(activationFailure);
        assertThat(runtimeLoader.activationStarted).isTrue();
        assertThat(runtimeToolCache.removedPath).isEqualTo(runtimeToolCache.installedPath);
        assertThat(runtimeLoader.deactivatedModuleId).isNull();
    }

    @Test
    void installSuppressesRuntimeCacheRemovalFailureWhenActivationFails() {
        RuntimeException activationFailure = new RuntimeException("activation failed");
        FailingActivationRuntimeLoader runtimeLoader = new FailingActivationRuntimeLoader(activationFailure);
        FailingRemoveRuntimeToolCache runtimeToolCache = new FailingRemoveRuntimeToolCache();
        ArtifactInstaller installer = new ArtifactInstaller(
                new NoOpPublicationRecordVerifier(),
                new NoOpInstallPolicyEvaluator(),
                runtimeToolCache,
                runtimeLoader,
                new FailingRegistrationStore(),
                new FixedToolRegistry("sample.echo")
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> installer.install(publication(), "sample.echo", new McpCallContext("Bearer dev-admin", null, null, "req-1"))
        );

        assertThat(failure).isSameAs(activationFailure);
        assertThat(runtimeToolCache.removedPath).isEqualTo(runtimeToolCache.installedPath);
        assertThat(runtimeLoader.deactivatedModuleId).isNull();
        assertThat(failure.getSuppressed()).containsExactly(runtimeToolCache.removeFailure);
    }

    @Test
    void installSuppressesDeactivationAndRuntimeCacheRemovalFailuresWhenRegistrationSaveFails() {
        ToolModuleId moduleId = new ToolModuleId("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        RuntimeException deactivationFailure = new RuntimeException("deactivation failed");
        FailingDeactivationRuntimeLoader runtimeLoader = new FailingDeactivationRuntimeLoader(
                moduleId,
                "sample.echo",
                deactivationFailure
        );
        FailingRemoveRuntimeToolCache runtimeToolCache = new FailingRemoveRuntimeToolCache();
        FailingRegistrationStore registrationStore = new FailingRegistrationStore();
        ArtifactInstaller installer = new ArtifactInstaller(
                new NoOpPublicationRecordVerifier(),
                new NoOpInstallPolicyEvaluator(),
                runtimeToolCache,
                runtimeLoader,
                registrationStore,
                new FixedToolRegistry("sample.echo")
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> installer.install(publication(), "sample.echo", new McpCallContext("Bearer dev-admin", null, null, "req-1"))
        );

        assertThat(failure).isSameAs(registrationStore.failure);
        assertThat(runtimeLoader.deactivatedModuleId).isEqualTo(moduleId);
        assertThat(runtimeToolCache.removedPath).isEqualTo(runtimeToolCache.installedPath);
        assertThat(failure.getSuppressed()).containsExactly(deactivationFailure, runtimeToolCache.removeFailure);
    }

    @Test
    void installMarksSavedRegistrationRolledBackWhenFinalRegistryVersionFails() {
        ToolModuleId moduleId = new ToolModuleId("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        RecordingRuntimeLoader runtimeLoader = new RecordingRuntimeLoader(moduleId, "sample.echo");
        RecordingRuntimeToolCache runtimeToolCache = new RecordingRuntimeToolCache();
        RecordingRegistrationStore registrationStore = new RecordingRegistrationStore();
        FailingRegistryVersionToolRegistry toolRegistry = new FailingRegistryVersionToolRegistry("sample.echo");
        ArtifactInstaller installer = new ArtifactInstaller(
                new NoOpPublicationRecordVerifier(),
                new NoOpInstallPolicyEvaluator(),
                runtimeToolCache,
                runtimeLoader,
                registrationStore,
                toolRegistry
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> installer.install(publication(), "sample.echo", new McpCallContext("Bearer dev-admin", null, null, "req-1"))
        );

        assertThat(failure).isSameAs(toolRegistry.failure);
        assertThat(registrationStore.savedRecord).isNotNull();
        assertThat(registrationStore.markedRegistrationId).isEqualTo(registrationStore.savedRecord.registrationId());
        assertThat(registrationStore.markedStatus).isEqualTo("install-rolled-back");
        assertThat(registrationStore.findActive("sample.echo")).isEmpty();
        assertThat(runtimeLoader.deactivatedModuleId).isEqualTo(moduleId);
        assertThat(runtimeToolCache.removedPath).isEqualTo(runtimeToolCache.installedPath);
    }

    private static ArtifactPublicationRecord publication() {
        return new ArtifactPublicationRecord(
                new ArtifactCoordinate("dev.mrk.tools", "sample-module", "0.0.1-SNAPSHOT", null, "jar"),
                MeshingressArtifactType.TOOL_MODULE,
                ArtifactTrustStatus.APPROVED_LIMITED,
                "meshingress-repository://artifact/dev/mrk/tools/sample-module/0.0.1-SNAPSHOT/sample-module.jar",
                ArtifactChecksum.sha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
                new ArtifactScopeDeclaration(List.of("USER_WRITE"), List.of("USER_WRITE"), List.of("USER_WRITE"), List.of()),
                new ArtifactAssessmentSummary("approved", List.of("embedded"), 0, Map.of()),
                ArtifactProvenance.empty(),
                false,
                OffsetDateTime.parse("2026-06-18T10:30:00-04:00"),
                "test-key",
                "HmacSHA256",
                "signature"
        );
    }

    private static class NoOpPublicationRecordVerifier extends PublicationRecordVerifier {
        NoOpPublicationRecordVerifier() {
            super(null, null);
        }

        @Override
        public void verifySignature(ArtifactPublicationRecord publication) {
        }
    }

    private static class NoOpInstallPolicyEvaluator extends InstallPolicyEvaluator {
        NoOpInstallPolicyEvaluator() {
            super(null);
        }

        @Override
        public void requireInstallable(ArtifactPublicationRecord publication) {
        }

        @Override
        public void requireApprovedScopes(ArtifactPublicationRecord publication, List<McpFunctionDescriptor> functions) {
        }
    }

    private static class RecordingRuntimeToolCache extends RuntimeToolCache {
        final Path installedPath = Path.of("build", "runtime-cache", "sample-module.jar");
        Path removedPath;

        RecordingRuntimeToolCache() {
            super(null, null);
        }

        @Override
        public Path install(ArtifactPublicationRecord publication) {
            return installedPath;
        }

        @Override
        public void remove(Path cachedJar) {
            this.removedPath = cachedJar;
        }
    }

    private static class FailingRemoveRuntimeToolCache extends RecordingRuntimeToolCache {
        private final RuntimeException removeFailure = new RuntimeException("runtime cache removal failed");

        @Override
        public void remove(Path cachedJar) {
            super.remove(cachedJar);
            throw removeFailure;
        }
    }

    private static class RecordingRuntimeLoader implements ToolRuntimeLoader {
        private final ToolModuleId moduleId;
        private final List<String> registeredFunctions;
        ToolModuleId deactivatedModuleId;

        RecordingRuntimeLoader(ToolModuleId moduleId, String registeredFunction) {
            this.moduleId = moduleId;
            this.registeredFunctions = List.of(registeredFunction);
        }

        @Override
        public ToolModuleHandle activate(ToolArtifactSource source) {
            return handle();
        }

        @Override
        public ToolModuleHandle activate(ResolvedToolArtifact artifact) {
            return handle();
        }

        @Override
        public void deactivate(ToolModuleId moduleId) {
            this.deactivatedModuleId = moduleId;
        }

        @Override
        public Optional<ToolModuleStatus> status(ToolModuleId moduleId) {
            return Optional.empty();
        }

        @Override
        public List<ToolModuleStatus> list() {
            return List.of();
        }

        private ToolModuleHandle handle() {
            return new ToolModuleHandle(moduleId, ToolModuleState.ACTIVE, OffsetDateTime.now(), registeredFunctions);
        }
    }

    private static class FailingActivationRuntimeLoader extends RecordingRuntimeLoader {
        private final RuntimeException failure;

        FailingActivationRuntimeLoader(RuntimeException failure) {
            super(new ToolModuleId("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT"), "sample.echo");
            this.failure = failure;
        }

        @Override
        public ToolModuleHandle activate(ToolArtifactSource source) {
            throw failure;
        }
    }

    private static class PartiallyActivatingRuntimeLoader extends RecordingRuntimeLoader {
        private final RuntimeException failure;
        private boolean activationStarted;

        PartiallyActivatingRuntimeLoader(RuntimeException failure) {
            super(new ToolModuleId("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT"), "sample.echo");
            this.failure = failure;
        }

        @Override
        public ToolModuleHandle activate(ToolArtifactSource source) {
            activationStarted = true;
            throw failure;
        }
    }

    private static class FailingDeactivationRuntimeLoader extends RecordingRuntimeLoader {
        private final RuntimeException failure;

        FailingDeactivationRuntimeLoader(ToolModuleId moduleId, String registeredFunction, RuntimeException failure) {
            super(moduleId, registeredFunction);
            this.failure = failure;
        }

        @Override
        public void deactivate(ToolModuleId moduleId) {
            super.deactivate(moduleId);
            throw failure;
        }
    }

    private static class FailingRegistrationStore implements ToolRegistrationStore {
        private final RuntimeException failure = new RuntimeException("registration save failed");

        @Override
        public Optional<ToolRegistrationRecord> findActive(String toolId, ToolRegistrationPhase phase) {
            return Optional.empty();
        }

        @Override
        public List<ToolRegistrationRecord> findActive(String toolId) {
            return List.of();
        }

        @Override
        public ToolRegistrationRecord saveActive(ToolRegistrationRecord record) {
            throw failure;
        }

        @Override
        public void markReplaced(String registrationId) {
        }

        @Override
        public ToolRegistrationRecord markStatus(String registrationId, String status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ToolRegistrationRecord> list() {
            return List.of();
        }
    }

    private static class RecordingRegistrationStore implements ToolRegistrationStore {
        private ToolRegistrationRecord savedRecord;
        private String markedRegistrationId;
        private String markedStatus;

        @Override
        public Optional<ToolRegistrationRecord> findActive(String toolId, ToolRegistrationPhase phase) {
            return activeRecords().stream()
                    .filter(record -> record.toolId().equals(toolId))
                    .filter(record -> record.phase() == phase)
                    .findFirst();
        }

        @Override
        public List<ToolRegistrationRecord> findActive(String toolId) {
            return activeRecords().stream()
                    .filter(record -> record.toolId().equals(toolId))
                    .toList();
        }

        @Override
        public ToolRegistrationRecord saveActive(ToolRegistrationRecord record) {
            this.savedRecord = record;
            return record;
        }

        @Override
        public void markReplaced(String registrationId) {
            markStatus(registrationId, "replaced");
        }

        @Override
        public ToolRegistrationRecord markStatus(String registrationId, String status) {
            this.markedRegistrationId = registrationId;
            this.markedStatus = status;
            savedRecord = new ToolRegistrationRecord(
                    savedRecord.registrationId(),
                    savedRecord.toolId(),
                    savedRecord.phase(),
                    savedRecord.sourceKind(),
                    status,
                    savedRecord.source(),
                    savedRecord.actor(),
                    savedRecord.requestId(),
                    savedRecord.registeredAt(),
                    savedRecord.replacedRegistrationId(),
                    savedRecord.runtimeModuleId(),
                    savedRecord.registeredFunctions()
            );
            return savedRecord;
        }

        @Override
        public List<ToolRegistrationRecord> list() {
            return savedRecord == null ? List.of() : List.of(savedRecord);
        }

        private List<ToolRegistrationRecord> activeRecords() {
            return list().stream()
                    .filter(record -> switch (record.status()) {
                        case "active", "reconciled", "installed-restart-required", "already-installed-restart-required" -> true;
                        default -> false;
                    })
                    .toList();
        }
    }

    private static class FixedToolRegistry implements ToolRegistry {
        private final McpFunctionDescriptor function;

        FixedToolRegistry(String functionName) {
            this.function = new McpFunctionDescriptor(
                    functionName,
                    "Sample Echo",
                    "Sample echo function",
                    1,
                    true,
                    ToolVisibility.PUBLIC,
                    null,
                    null,
                    null,
                    null,
                    true
            );
        }

        @Override
        public List<McpToolDescriptor> listPublicEnabledTools() {
            return List.of();
        }

        @Override
        public List<McpFunctionDescriptor> listPublicEnabledFunctions() {
            return List.of(function);
        }

        @Override
        public List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate) {
            return List.of(new McpToolDescriptor(
                    "sample",
                    "Sample",
                    "Sample tool",
                    1,
                    true,
                    ToolVisibility.PUBLIC,
                    List.of(function),
                    null,
                    true
            ));
        }

        @Override
        public Optional<McpToolDescriptor> findEnabledTool(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<McpFunctionDescriptor> findEnabledFunction(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<McpToolDescriptor> findOwningTool(String functionName) {
            return Optional.empty();
        }

        @Override
        public Optional<McpToolDescriptor> findTool(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<McpToolHandler> findHandler(String handlerKey) {
            return Optional.empty();
        }

        @Override
        public ToolCheckResult check(McpToolDescriptor descriptor, boolean updateMode) {
            return null;
        }

        @Override
        public McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context) {
            return descriptor;
        }

        @Override
        public McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner) {
            return handler.descriptor();
        }

        @Override
        public void unregisterRuntimeOwner(String owner) {
        }

        @Override
        public McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context) {
            return null;
        }

        @Override
        public McpToolDescriptor disable(String name, McpCallContext context) {
            return null;
        }

        @Override
        public long registryVersion() {
            return 1;
        }

        @Override
        public List<ToolAuditEvent> auditEvents() {
            return List.of();
        }
    }

    private static class FailingRegistryVersionToolRegistry extends FixedToolRegistry {
        private final RuntimeException failure = new RuntimeException("registry version failed");

        FailingRegistryVersionToolRegistry(String functionName) {
            super(functionName);
        }

        @Override
        public long registryVersion() {
            throw failure;
        }
    }
}
