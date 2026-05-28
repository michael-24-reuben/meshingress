package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.runtime.artifacts.LocalJarArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.LocalJarSource;
import dev.mrk.meshingress.runtime.artifacts.LocalMavenRepositoryArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.MavenCoordinatesSource;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolverChain;
import dev.mrk.meshingress.runtime.artifacts.ToolModuleDescriptor;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleState;
import dev.mrk.meshingress.runtime.loader.DefaultToolRuntimeLoader;
import dev.mrk.meshingress.runtime.loader.ToolModuleHandlerFactory;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import dev.mrk.meshingress.runtime.registry.ToolRegistrationBridge;
import dev.mrk.meshingress.runtime.registry.ToolModuleRegistration;
import dev.mrk.meshingress.runtime.spring.SpringToolModuleApplicationContextFactory;
import dev.mrk.meshingress.runtime.spring.UrlToolModuleClassLoaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(properties = "meshingress.architect.root=../../architect")
class ToolRuntimeLoaderSmokeTests {

    private static final String SAMPLE_FUNCTION = "helloworld.text";
    private static final String SAMPLE_GROUP_ID = "dev.mrk.toolspace";
    private static final String SAMPLE_ARTIFACT_ID = "sample-module";
    private static final String SAMPLE_VERSION = "0.0.1-SNAPSHOT";
    private static final String SAMPLE_JAR_NAME = "sample-module-0.0.1-SNAPSHOT-all.jar";

    @TempDir
    Path localRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ToolModuleHandlerFactory handlerFactory;

    @Test
    void loadsSampleJarThroughEveryRuntimeLoaderEntryPoint() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + SAMPLE_JAR_NAME);
        installSampleMavenArtifact(sampleJar);

        List<ActivationCase> activationCases = List.of(
                new ActivationCase(
                        "activate(ToolArtifactSource) with LocalJarSource",
                        loader -> loader.activate(new LocalJarSource(sampleJar))
                ),
                new ActivationCase(
                        "activate(ToolArtifactSource) with MavenCoordinatesSource",
                        loader -> loader.activate(new MavenCoordinatesSource(SAMPLE_GROUP_ID, SAMPLE_ARTIFACT_ID, SAMPLE_VERSION, List.of()))
                ),
                new ActivationCase(
                        "activate(ResolvedToolArtifact)",
                        loader -> loader.activate(resolvedSampleArtifact(sampleJar))
                )
        );

        for (ActivationCase activationCase : activationCases) {
            RuntimeLoaderFixture fixture = newLoader();
            ToolModuleHandle handle = null;
            try {
                handle = activationCase.activate(fixture.loader());
                assertActivated(fixture, handle, activationCase.name());
            } finally {
                deactivateIfActive(fixture.loader(), handle);
            }
            assertUnloaded(fixture, handle, activationCase.name());
        }
    }

    private RuntimeLoaderFixture newLoader() {
        ToolArtifactResolver resolver = new ToolArtifactResolverChain(List.of(
                new LocalMavenRepositoryArtifactResolver(),
                new LocalJarArtifactResolver()
        ));
        RecordingToolRegistrationBridge registrationBridge = new RecordingToolRegistrationBridge();
        ToolRuntimeLoader loader = new DefaultToolRuntimeLoader(
                resolver,
                new ToolArtifactResolutionContext(localRepository, null, false, List.of()),
                new UrlToolModuleClassLoaderFactory(),
                new SpringToolModuleApplicationContextFactory(),
                handlerFactory,
                registrationBridge,
                applicationContext
        );
        return new RuntimeLoaderFixture(loader, registrationBridge);
    }

    private void assertActivated(RuntimeLoaderFixture fixture, ToolModuleHandle handle, String caseName) {
        assertThat(handle.state())
                .as(caseName + " handle state")
                .isEqualTo(ToolModuleState.ACTIVE);
        assertThat(handle.registeredFunctions())
                .as(caseName + " registered functions")
                .contains(SAMPLE_FUNCTION);
        assertThat(fixture.registrationBridge().registeredFunctions(handle.moduleId()))
                .as(caseName + " bridge registry function")
                .contains(SAMPLE_FUNCTION);
        assertThat(fixture.loader().status(handle.moduleId()))
                .as(caseName + " runtime status")
                .hasValueSatisfying(status -> {
                    assertThat(status.state()).isEqualTo(ToolModuleState.ACTIVE);
                    assertThat(status.registeredFunctions()).contains(SAMPLE_FUNCTION);
                });
        assertThat(fixture.loader().list())
                .as(caseName + " runtime list")
                .anySatisfy(status -> assertThat(status.moduleId()).isEqualTo(handle.moduleId()));
    }

    private void deactivateIfActive(ToolRuntimeLoader loader, ToolModuleHandle handle) {
        if (handle == null) {
            return;
        }
        Optional<ToolModuleState> state = loader.status(handle.moduleId()).map(status -> status.state());
        if (state.isPresent() && state.get() == ToolModuleState.ACTIVE) {
            loader.deactivate(handle.moduleId());
        }
    }

    private void assertUnloaded(RuntimeLoaderFixture fixture, ToolModuleHandle handle, String caseName) {
        if (handle == null) {
            return;
        }
        assertThat(fixture.loader().status(handle.moduleId()))
                .as(caseName + " unloaded status")
                .hasValueSatisfying(status -> assertThat(status.state()).isEqualTo(ToolModuleState.UNLOADED));
        assertThat(fixture.registrationBridge().registeredFunctions(handle.moduleId()))
                .as(caseName + " bridge registry cleanup")
                .isEmpty();
    }

    private ResolvedToolArtifact resolvedSampleArtifact(Path sampleJar) {
        ToolModuleId moduleId = new ToolModuleId("smoke:" + SAMPLE_ARTIFACT_ID + ":resolved");
        return new ResolvedToolArtifact(
                moduleId,
                sampleJar,
                List.of(sampleJar),
                new LocalJarSource(sampleJar),
                Map.of(),
                new ToolModuleDescriptor(
                        moduleId,
                        SAMPLE_JAR_NAME,
                        SAMPLE_VERSION,
                        List.of("dev.mrk.toolspace.sample.SampleToolAutoConfiguration"),
                        Map.of("source", "smoke-test")
                )
        );
    }

    private void installSampleMavenArtifact(Path sampleJar) throws IOException {
        Path artifactDirectory = localRepository
                .resolve(SAMPLE_GROUP_ID.replace('.', '/'))
                .resolve(SAMPLE_ARTIFACT_ID)
                .resolve(SAMPLE_VERSION);
        Files.createDirectories(artifactDirectory);
        Files.copy(
                sampleJar,
                artifactDirectory.resolve(SAMPLE_ARTIFACT_ID + "-" + SAMPLE_VERSION + ".jar"),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.writeString(
                artifactDirectory.resolve(SAMPLE_ARTIFACT_ID + "-" + SAMPLE_VERSION + ".pom"),
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>dev.mrk.toolspace</groupId>
                          <artifactId>sample-module</artifactId>
                          <version>0.0.1-SNAPSHOT</version>
                        </project>
                        """
        );
    }

    private Path sampleJar() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return List.of(
                        current.resolve("temp").resolve(SAMPLE_JAR_NAME),
                        current.getParent().resolve("temp").resolve(SAMPLE_JAR_NAME),
                        current.getParent().getParent().resolve("temp").resolve(SAMPLE_JAR_NAME)
                )
                .stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(current.resolve("temp").resolve(SAMPLE_JAR_NAME));
    }

    private record ActivationCase(String name, Activation activation) {
        ToolModuleHandle activate(ToolRuntimeLoader loader) throws Exception {
            return activation.activate(loader);
        }
    }

    @FunctionalInterface
    private interface Activation {
        ToolModuleHandle activate(ToolRuntimeLoader loader) throws Exception;
    }

    private record RuntimeLoaderFixture(
            ToolRuntimeLoader loader,
            RecordingToolRegistrationBridge registrationBridge
    ) {
    }

    private static final class RecordingToolRegistrationBridge implements ToolRegistrationBridge {

        private final Map<ToolModuleId, List<String>> registeredFunctions = new LinkedHashMap<>();

        @Override
        public ToolModuleRegistration register(ToolModuleId moduleId, List<McpToolHandler> handlers) {
            List<String> functions = handlers.stream()
                    .flatMap(handler -> handler.descriptor().functions().stream())
                    .map(function -> function.name())
                    .toList();
            registeredFunctions.put(moduleId, functions);
            return new ToolModuleRegistration(functions);
        }

        @Override
        public void unregister(ToolModuleId moduleId) {
            registeredFunctions.remove(moduleId);
        }

        List<String> registeredFunctions(ToolModuleId moduleId) {
            return registeredFunctions.getOrDefault(moduleId, List.of());
        }
    }
}
