package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
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

    private MeshingressProperties meshingressProperties(Path bundlePom) {
        MeshingressProperties defaults = new MeshingressProperties(null, null, null, null, null, null, null, null, null, null);
        MeshingressProperties.Tools defaultsTools = defaults.tools();
        MeshingressProperties.Tools.Registration defaultsRegistration = defaultsTools.registration();
        MeshingressProperties.Tools.Registration registration = new MeshingressProperties.Tools.Registration(
                defaultsRegistration.enabled(),
                defaultsRegistration.allowExperimental(),
                defaultsRegistration.allowStaging(),
                defaultsRegistration.allowBundle(),
                defaultsRegistration.allowNativeHttp(),
                defaultsRegistration.experimentalReplaceExisting(),
                defaultsRegistration.allowExperimentalOverrideBundle(),
                defaultsRegistration.allowExperimentalOverrideStaging(),
                defaultsRegistration.allowStagingOverrideBundle(),
                defaultsRegistration.allowOverrideNative(),
                defaultsRegistration.stagingConflictPolicy(),
                tempDir.resolve("tools").toString(),
                tempDir.resolve("repository").toString(),
                bundlePom.toString(),
                defaultsRegistration.requireLocalJarChecksum(),
                defaultsRegistration.requireMavenVersionPin(),
                defaultsRegistration.requireApprovalForDynamicPhases()
        );
        MeshingressProperties.Tools tools = new MeshingressProperties.Tools(
                defaultsTools.registry(),
                registration,
                defaultsTools.allowList(),
                defaultsTools.denyList(),
                defaultsTools.defaultTimeout(),
                defaultsTools.defaultAudit(),
                defaultsTools.defaultDebugTrace()
        );
        return new MeshingressProperties(
                defaults.identity(),
                defaults.mcp(),
                tools,
                defaults.dispatch(),
                defaults.cache(),
                defaults.security(),
                defaults.scopes(),
                defaults.audit(),
                defaults.secrets(),
                defaults.repository()
        );
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
