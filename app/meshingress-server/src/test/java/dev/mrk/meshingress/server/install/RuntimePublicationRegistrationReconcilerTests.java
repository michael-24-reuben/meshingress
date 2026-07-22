package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationPhase;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationStore;
import dev.mrk.meshingress.controller.roles.registration.ToolSourceKind;
import dev.mrk.meshingress.artifact.storage.ProjectRootResolver;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleState;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleStatus;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePublicationRegistrationReconcilerTests {

    @TempDir
    Path tempDir;

    @Test
    void reconcilesActivePublicationRegistrationFromCachedJar() throws Exception {
        Path projectRoot = projectRoot();
        Path cachedJar = projectRoot.resolve("repository/runtime-cache/sample-module.jar");
        Files.createDirectories(cachedJar.getParent());
        Files.writeString(cachedJar, "jar bytes");
        RecordingRegistrationStore registrationStore = new RecordingRegistrationStore(registration("active", projectRoot, cachedJar));
        RecordingRuntimeLoader runtimeLoader = new RecordingRuntimeLoader();
        RuntimePublicationRegistrationReconciler reconciler =
                new RuntimePublicationRegistrationReconciler(registrationStore, runtimeLoader, projectRoot);

        RuntimePublicationRegistrationReconciler.RuntimePublicationReconciliationResult result = reconciler.reconcile();

        assertThat(result.candidates()).isEqualTo(1);
        assertThat(result.reconciled()).isEqualTo(1);
        assertThat(result.missingCache()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(runtimeLoader.activatedSource).isNotNull();
        assertThat(runtimeLoader.activatedSource.jarPath()).isEqualTo(cachedJar.toAbsolutePath().normalize());
        assertThat(registrationStore.record.status()).isEqualTo("reconciled");
    }

    @Test
    void marksPublicationRegistrationMissingCacheWhenCachedJarIsGone() throws Exception {
        Path projectRoot = projectRoot();
        Path missingJar = projectRoot.resolve("repository/runtime-cache/missing-module.jar");
        RecordingRegistrationStore registrationStore = new RecordingRegistrationStore(registration("active", projectRoot, missingJar));
        RecordingRuntimeLoader runtimeLoader = new RecordingRuntimeLoader();
        RuntimePublicationRegistrationReconciler reconciler =
                new RuntimePublicationRegistrationReconciler(registrationStore, runtimeLoader, projectRoot);

        RuntimePublicationRegistrationReconciler.RuntimePublicationReconciliationResult result = reconciler.reconcile();

        assertThat(result.candidates()).isEqualTo(1);
        assertThat(result.reconciled()).isZero();
        assertThat(result.missingCache()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(runtimeLoader.activatedSource).isNull();
        assertThat(registrationStore.record.status()).isEqualTo("missing-cache");
    }

    @Test
    void rejectsAbsoluteRuntimeCachePaths() throws Exception {
        Path projectRoot = projectRoot();
        Path outsideJar = tempDir.resolve("outside-cache/sample-module.jar");
        Files.createDirectories(outsideJar.getParent());
        Files.writeString(outsideJar, "jar bytes");
        Map<String, String> source = new LinkedHashMap<>();
        source.put("coordinate", "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        source.put("runtimeCachePath", outsideJar.toString());
        RecordingRegistrationStore registrationStore = new RecordingRegistrationStore(record("active", source));
        RecordingRuntimeLoader runtimeLoader = new RecordingRuntimeLoader();

        RuntimePublicationRegistrationReconciler.RuntimePublicationReconciliationResult result =
                new RuntimePublicationRegistrationReconciler(registrationStore, runtimeLoader, projectRoot).reconcile();

        assertThat(result.missingCache()).isEqualTo(1);
        assertThat(runtimeLoader.activatedSource).isNull();
        assertThat(registrationStore.record.status()).isEqualTo("missing-cache");
    }

    private ToolRegistrationRecord registration(String status, Path projectRoot, Path cachedJar) {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("coordinate", "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        source.put("runtimeCachePath", ProjectRootResolver.relativize(projectRoot, cachedJar));
        return record(status, source);
    }

    private ToolRegistrationRecord record(String status, Map<String, String> source) {
        return new ToolRegistrationRecord(
                "publication:sample-module:test",
                "sample.echo",
                ToolRegistrationPhase.STAGING,
                ToolSourceKind.PUBLICATION_RECORD,
                status,
                source,
                "test",
                "req-1",
                OffsetDateTime.parse("2026-06-21T10:00:00-04:00"),
                null,
                "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT",
                List.of("sample.echo")
        );
    }

    private Path projectRoot() throws Exception {
        Path projectRoot = tempDir.resolve("meshingress");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("pom.xml"), "<project><artifactId>meshingress</artifactId></project>");
        return projectRoot;
    }

    private static class RecordingRegistrationStore implements ToolRegistrationStore {
        private ToolRegistrationRecord record;

        RecordingRegistrationStore(ToolRegistrationRecord record) {
            this.record = record;
        }

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
            this.record = record;
            return record;
        }

        @Override
        public void markReplaced(String registrationId) {
            markStatus(registrationId, "replaced");
        }

        @Override
        public ToolRegistrationRecord markStatus(String registrationId, String status) {
            assertThat(registrationId).isEqualTo(record.registrationId());
            record = new ToolRegistrationRecord(
                    record.registrationId(),
                    record.toolId(),
                    record.phase(),
                    record.sourceKind(),
                    status,
                    record.source(),
                    record.actor(),
                    record.requestId(),
                    record.registeredAt(),
                    record.replacedRegistrationId(),
                    record.runtimeModuleId(),
                    record.registeredFunctions()
            );
            return record;
        }

        @Override
        public List<ToolRegistrationRecord> list() {
            return List.of(record);
        }
    }

    private static class RecordingRuntimeLoader implements ToolRuntimeLoader {
        private dev.mrk.meshingress.runtime.artifacts.LocalJarSource activatedSource;

        @Override
        public ToolModuleHandle activate(ToolArtifactSource source) {
            activatedSource = (dev.mrk.meshingress.runtime.artifacts.LocalJarSource) source;
            return new ToolModuleHandle(
                    new ToolModuleId("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT"),
                    ToolModuleState.ACTIVE,
                    OffsetDateTime.now(),
                    List.of("sample.echo")
            );
        }

        @Override
        public ToolModuleHandle activate(ResolvedToolArtifact artifact) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deactivate(ToolModuleId moduleId) {
        }

        @Override
        public Optional<ToolModuleStatus> status(ToolModuleId moduleId) {
            return Optional.empty();
        }

        @Override
        public List<ToolModuleStatus> list() {
            return List.of();
        }
    }
}
