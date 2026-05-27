package dev.mrk.meshingress.runtime.artifacts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMavenRepositoryArtifactResolverTests {

    @TempDir
    Path localRepository;

    @Test
    void resolvesInstalledArtifactAndRuntimeDependencies() throws Exception {
        installArtifact(
                "dev.mrk.toolspace",
                "sample-tool",
                "1.0.0",
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>dev.mrk.toolspace</groupId>
                          <artifactId>sample-tool</artifactId>
                          <version>1.0.0</version>
                          <dependencies>
                            <dependency>
                              <groupId>example.libs</groupId>
                              <artifactId>helper</artifactId>
                              <version>2.0.0</version>
                            </dependency>
                            <dependency>
                              <groupId>example.libs</groupId>
                              <artifactId>test-only</artifactId>
                              <version>2.0.0</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
                        </project>
                        """
        );
        installArtifact(
                "example.libs",
                "helper",
                "2.0.0",
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>example.libs</groupId>
                          <artifactId>helper</artifactId>
                          <version>2.0.0</version>
                        </project>
                        """
        );
        installArtifact(
                "example.libs",
                "test-only",
                "2.0.0",
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>example.libs</groupId>
                          <artifactId>test-only</artifactId>
                          <version>2.0.0</version>
                        </project>
                        """
        );

        LocalMavenRepositoryArtifactResolver resolver = new LocalMavenRepositoryArtifactResolver();
        ResolvedToolArtifact artifact = resolver.resolve(
                new MavenCoordinatesSource("dev.mrk.toolspace", "sample-tool", "1.0.0", List.of()),
                new ToolArtifactResolutionContext(localRepository, null, false, List.of())
        );

        assertThat(artifact.moduleId().value()).isEqualTo("dev.mrk.toolspace:sample-tool:1.0.0");
        assertThat(artifact.mainJar().getFileName().toString()).isEqualTo("sample-tool-1.0.0.jar");
        assertThat(artifact.runtimeClasspath())
                .extracting(path -> path.getFileName().toString())
                .containsExactly("sample-tool-1.0.0.jar", "helper-2.0.0.jar");
    }

    private void installArtifact(String groupId, String artifactId, String version, String pomXml) throws Exception {
        Path base = localRepository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(base);
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(base.resolve(artifactId + "-" + version + ".jar")))) {
        }
        Files.writeString(base.resolve(artifactId + "-" + version + ".pom"), pomXml);
    }
}
/*
app/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/BundleSpec.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/LocalJarSpec.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/MavenCoordinatesSpec.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/NativeSpec.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationContext.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationErrors.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationPhase.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationRecord.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationRequest.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationResult.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolSourceKind.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/InMemoryToolRegistrationStore.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationIds.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationStore.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationStrategy.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ToolRegistrationService.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ExperimentalToolRegistrationStrategy.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/StagingToolRegistrationStrategy.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/BundleToolRegistrationStrategy.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/NativeToolRegistrationStrategy.java
app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RoleToolService.java
app/meshingress-server/src/main/resources/application.properties
app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpControllerTests.java
* app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ExperimentalToolRegistrationStrategy.java
*/