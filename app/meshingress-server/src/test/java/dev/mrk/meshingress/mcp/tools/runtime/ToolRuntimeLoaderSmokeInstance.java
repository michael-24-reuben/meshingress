package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.tools.registry.scan-on-startup=false"
})
class ToolRuntimeLoaderSmokeTestResults {

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

    @Autowired
    private ToolRegistrationBridge registrationBridge;

    @Autowired
    private ToolRegistry toolRegistry;

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
                        loader -> loader.activate(new MavenCoordinatesSource(
                                SAMPLE_GROUP_ID,
                                SAMPLE_ARTIFACT_ID,
                                SAMPLE_VERSION,
                                List.of()
                        ))
                ),
                new ActivationCase(
                        "activate(ResolvedToolArtifact)",
                        loader -> loader.activate(resolvedSampleArtifact(sampleJar))
                )
        );

        System.out.println();
        System.out.println("=== ToolRuntimeLoader smoke results ===");
        System.out.println("sampleJar=" + sampleJar);
        System.out.println("localRepository=" + localRepository);

        for (ActivationCase activationCase : activationCases) {
            ToolRuntimeLoader loader = newLoader();
            ToolModuleHandle handle = null;

            System.out.println();
            System.out.println("--- " + activationCase.name() + " ---");

            try {
                handle = activationCase.activate(loader);
                printActivated(loader, handle, activationCase.name());
            } catch (Exception ex) {
                System.out.println("activation.success=false");
                System.out.println("activation.errorType=" + ex.getClass().getName());
                System.out.println("activation.errorMessage=" + ex.getMessage());
                throw ex;
            } finally {
                deactivateIfActive(loader, handle);
                printUnloaded(loader, handle, activationCase.name());
            }
        }

        System.out.println();
        System.out.println("=== ToolRuntimeLoader smoke results complete ===");
    }

    private ToolRuntimeLoader newLoader() {
        ToolArtifactResolver resolver = new ToolArtifactResolverChain(List.of(
                new LocalMavenRepositoryArtifactResolver(),
                new LocalJarArtifactResolver()
        ));

        return new DefaultToolRuntimeLoader(
                resolver,
                new ToolArtifactResolutionContext(localRepository, null, false, List.of()),
                new UrlToolModuleClassLoaderFactory(),
                new SpringToolModuleApplicationContextFactory(),
                handlerFactory,
                registrationBridge,
                applicationContext
        );
    }

    private void printActivated(ToolRuntimeLoader loader, ToolModuleHandle handle, String caseName) {
        System.out.println("activation.success=true");
        System.out.println("caseName=" + caseName);
        System.out.println("moduleId=" + handle.moduleId());
        System.out.println("handle.state=" + handle.state());
        System.out.println("handle.registeredFunctions=" + handle.registeredFunctions());
        System.out.println("handle.hasSampleFunction=" + handle.registeredFunctions().contains(SAMPLE_FUNCTION));
        System.out.println("registry.hasSampleFunction=" + toolRegistry.findEnabledFunction(SAMPLE_FUNCTION).isPresent());

        Optional<?> status = loader.status(handle.moduleId());
        System.out.println("runtime.status.present=" + status.isPresent());

        loader.status(handle.moduleId()).ifPresent(runtimeStatus -> {
            System.out.println("runtime.status.state=" + runtimeStatus.state());
            System.out.println("runtime.status.registeredFunctions=" + runtimeStatus.registeredFunctions());
            System.out.println("runtime.status.hasSampleFunction="
                    + runtimeStatus.registeredFunctions().contains(SAMPLE_FUNCTION));
        });

        boolean listed = loader.list()
                .stream()
                .anyMatch(runtimeStatus -> runtimeStatus.moduleId().equals(handle.moduleId()));

        System.out.println("runtime.list.size=" + loader.list().size());
        System.out.println("runtime.list.hasModule=" + listed);
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

    private void printUnloaded(ToolRuntimeLoader loader, ToolModuleHandle handle, String caseName) {
        if (handle == null) {
            System.out.println("unload.skipped=true");
            System.out.println("unload.reason=no handle");
            return;
        }

        System.out.println("unload.caseName=" + caseName);
        System.out.println("unload.moduleId=" + handle.moduleId());

        Optional<ToolModuleState> state = loader.status(handle.moduleId()).map(status -> status.state());
        System.out.println("unload.status.present=" + state.isPresent());
        System.out.println("unload.status.state=" + state.orElse(null));
        System.out.println("unload.registry.hasSampleFunction="
                + toolRegistry.findEnabledFunction(SAMPLE_FUNCTION).isPresent());
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
}
