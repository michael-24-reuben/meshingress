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
