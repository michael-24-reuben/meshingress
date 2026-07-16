package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.artifact.storage.ProjectRootResolver;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.server.install.RuntimeToolCache;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleStatus;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistrationServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void deleteRemovesMavenBundleDependencyAddedByRegistration() throws Exception {
        Path bundlePom = tempDir.resolve("pom.xml");
        Files.writeString(bundlePom, """
                <project>
                  <dependencies>
                    <dependency>
                      <groupId>dev.mrk.toolspace</groupId>
                      <artifactId>sample-module</artifactId>
                      <version>0.0.1-SNAPSHOT</version>
                    </dependency>
                    <dependency>
                      <groupId>dev.mrk.toolspace</groupId>
                      <artifactId>other-module</artifactId>
                      <version>0.0.1-SNAPSHOT</version>
                    </dependency>
                  </dependencies>
                </project>
                """);

        ToolRegistrationStore store = new InMemoryToolRegistrationStore();
        store.saveActive(new ToolRegistrationRecord(
                "reg-1",
                "sample.tool",
                ToolRegistrationPhase.BUNDLE,
                ToolSourceKind.MAVEN_BUNDLE,
                "installed-restart-required",
                Map.of(
                        "groupId", "dev.mrk.toolspace",
                        "artifactId", "sample-module",
                        "version", "0.0.1-SNAPSHOT",
                        "bundlePomPath", bundlePom.toString(),
                        "dependencyAdded", "true"
                ),
                "test",
                "req-1",
                OffsetDateTime.now(),
                null,
                null,
                List.of("sample.tool")
        ));

        ToolRegistrationService service = new ToolRegistrationService(
                new ObjectMapper(),
                meshingressProperties(bundlePom),
                store,
                new EmptyToolRuntimeLoader(),
                new RecordingRuntimeToolCache(),
                new EmptyToolRegistry(),
                List.of()
        );

        ObjectNode result = service.delete(
                new McpCallContext("Bearer dev-admin", null, null, "req-1"),
                "sample.tool",
                "disable"
        );

        assertThat(result.path("deleted").asBoolean()).isTrue();
        assertThat(result.path("bundleDependenciesRemoved").asInt()).isEqualTo(1);
        assertThat(result.path("restartRequired").asBoolean()).isTrue();
        assertThat(result.path("registrations").path(0).path("status").asString())
                .isEqualTo("deleted-restart-required");

        String updatedPom = Files.readString(bundlePom);
        assertThat(updatedPom).doesNotContain("<artifactId>sample-module</artifactId>");
        assertThat(updatedPom).contains("<artifactId>other-module</artifactId>");
    }

    @Test
    void deleteDeactivatesPublicationRuntimeModuleAndRemovesRuntimeCache() throws Exception {
        ToolRegistrationStore store = new InMemoryToolRegistrationStore();
        Path projectRoot = projectRoot();
        Path cachedJar = projectRoot.resolve("repository/runtime-cache/sample-module.jar");
        store.saveActive(new ToolRegistrationRecord(
                "publication-reg-1",
                "sample.tool",
                ToolRegistrationPhase.STAGING,
                ToolSourceKind.PUBLICATION_RECORD,
                "active",
                Map.of("runtimeCachePath", ProjectRootResolver.relativize(projectRoot, cachedJar)),
                "test",
                "req-1",
                OffsetDateTime.now(),
                null,
                "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT",
                List.of("sample.tool")
        ));
        RecordingToolRuntimeLoader runtimeLoader = new RecordingToolRuntimeLoader();
        RecordingRuntimeToolCache runtimeToolCache = new RecordingRuntimeToolCache();
        ToolRegistrationService service = new ToolRegistrationService(
                new ObjectMapper(),
                meshingressProperties(tempDir.resolve("pom.xml")),
                store,
                runtimeLoader,
                runtimeToolCache,
                new EmptyToolRegistry(),
                List.of(),
                projectRoot
        );

        ObjectNode result = service.delete(
                new McpCallContext("Bearer dev-admin", null, null, "req-1"),
                "sample.tool",
                "disable"
        );

        assertThat(result.path("deleted").asBoolean()).isTrue();
        assertThat(result.path("runtimeDeactivated").asInt()).isEqualTo(1);
        assertThat(result.path("runtimeCacheRemoved").asInt()).isEqualTo(1);
        assertThat(result.path("registrations").path(0).path("status").asString()).isEqualTo("deleted");
        assertThat(result.path("registrations").path(0).path("runtimeDeactivated").asBoolean()).isTrue();
        assertThat(result.path("registrations").path(0).path("runtimeCacheRemoved").asBoolean()).isTrue();
        assertThat(runtimeLoader.deactivatedModuleId.value()).isEqualTo("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        assertThat(runtimeToolCache.removedPath).isEqualTo(cachedJar);
        assertThat(store.findActive("sample.tool")).isEmpty();
    }

    @Test
    void deleteDoesNotRemoveAnAbsolutePublicationRuntimeCachePath() throws Exception {
        ToolRegistrationStore store = new InMemoryToolRegistrationStore();
        Path projectRoot = projectRoot();
        Path outsideJar = tempDir.resolve("outside-cache/sample-module.jar");
        store.saveActive(new ToolRegistrationRecord(
                "publication-reg-absolute",
                "sample.tool",
                ToolRegistrationPhase.STAGING,
                ToolSourceKind.PUBLICATION_RECORD,
                "active",
                Map.of("runtimeCachePath", outsideJar.toString()),
                "test",
                "req-1",
                OffsetDateTime.now(),
                null,
                "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT",
                List.of("sample.tool")
        ));
        RecordingToolRuntimeLoader runtimeLoader = new RecordingToolRuntimeLoader();
        RecordingRuntimeToolCache runtimeToolCache = new RecordingRuntimeToolCache();
        ToolRegistrationService service = new ToolRegistrationService(
                new ObjectMapper(),
                meshingressProperties(tempDir.resolve("pom.xml")),
                store,
                runtimeLoader,
                runtimeToolCache,
                new EmptyToolRegistry(),
                List.of(),
                projectRoot
        );

        ObjectNode result = service.delete(
                new McpCallContext("Bearer dev-admin", null, null, "req-1"),
                "sample.tool",
                "disable"
        );

        assertThat(result.path("runtimeCacheRemoved").asInt()).isZero();
        assertThat(runtimeLoader.deactivatedModuleId.value()).isEqualTo("dev.mrk.tools:sample-module:0.0.1-SNAPSHOT");
        assertThat(runtimeToolCache.removedPath).isNull();
    }

    private Path projectRoot() throws Exception {
        Path projectRoot = tempDir.resolve("meshingress");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("pom.xml"), "<project><artifactId>meshingress</artifactId></project>");
        return projectRoot;
    }

    private MeshingressProperties meshingressProperties(Path ignoredBundlePom) {
        return new MeshingressProperties(null, null, null, null, null, null, null, null, null, null);
    }

    private static final class EmptyToolRuntimeLoader implements ToolRuntimeLoader {

        @Override
        public ToolModuleHandle activate(ToolArtifactSource source) {
            throw new UnsupportedOperationException("activate is not used by this test");
        }

        @Override
        public ToolModuleHandle activate(ResolvedToolArtifact artifact) {
            throw new UnsupportedOperationException("activate is not used by this test");
        }

        @Override
        public void deactivate(ToolModuleId moduleId) {
            throw new UnsupportedOperationException("deactivate is not used by this test");
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

    private static final class RecordingToolRuntimeLoader implements ToolRuntimeLoader {

        private ToolModuleId deactivatedModuleId;

        @Override
        public ToolModuleHandle activate(ToolArtifactSource source) {
            throw new UnsupportedOperationException("activate is not used by this test");
        }

        @Override
        public ToolModuleHandle activate(ResolvedToolArtifact artifact) {
            throw new UnsupportedOperationException("activate is not used by this test");
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
    }

    private static final class RecordingRuntimeToolCache extends RuntimeToolCache {

        private Path removedPath;

        RecordingRuntimeToolCache() {
            super(null, null);
        }

        @Override
        public void remove(Path cachedJar) {
            this.removedPath = cachedJar;
        }
    }

    private static final class EmptyToolRegistry implements ToolRegistry {

        @Override
        public List<McpToolDescriptor> listPublicEnabledTools() {
            return List.of();
        }

        @Override
        public List<McpFunctionDescriptor> listPublicEnabledFunctions() {
            return List.of();
        }

        @Override
        public List<McpToolDescriptor> listRoleVisibleTools(boolean includeDisabled, boolean includePrivate) {
            return List.of();
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
            throw new UnsupportedOperationException("check is not used by this test");
        }

        @Override
        public McpToolDescriptor register(McpToolDescriptor descriptor, McpCallContext context) {
            throw new UnsupportedOperationException("register is not used by this test");
        }

        @Override
        public McpToolDescriptor registerRuntimeHandler(McpToolHandler handler, String owner) {
            throw new UnsupportedOperationException("registerRuntimeHandler is not used by this test");
        }

        @Override
        public void unregisterRuntimeOwner(String owner) {
            throw new UnsupportedOperationException("unregisterRuntimeOwner is not used by this test");
        }

        @Override
        public McpToolDescriptor update(String name, McpToolPatch patch, McpCallContext context) {
            throw new UnsupportedOperationException("update is not used by this test");
        }

        @Override
        public McpToolDescriptor disable(String name, McpCallContext context) {
            throw new UnsupportedOperationException("disable is not used by this test");
        }

        @Override
        public long registryVersion() {
            return 7;
        }

        @Override
        public List<ToolAuditEvent> auditEvents() {
            return List.of();
        }
    }
}
