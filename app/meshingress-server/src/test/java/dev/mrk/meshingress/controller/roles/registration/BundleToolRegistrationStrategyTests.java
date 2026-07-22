package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BundleToolRegistrationStrategyTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesEmbeddedPomAlongsideTheMavenJarAndUpdatesTheBundle() throws Exception {
        Path localRoot = temporaryDirectory.resolve("tools");
        Path repository = temporaryDirectory.resolve("repository/artifacts");
        Path bundlePom = writeBundlePom();
        MavenCoordinatesSpec coordinates = coordinates("1.0.0");
        String publishedPom = publishedPom(coordinates, "dev.mrk.private", "private-support", "2.0.0");
        Path jar = jarWithPom(localRoot, "tool.jar", publishedPom, "example/Tool.class");

        ToolRegistrationResult result = strategy(localRoot, repository, bundlePom)
                .register(request(jar, coordinates), context());

        Path artifactDirectory = repository.resolve("dev/mrk/tools/sample-tool/1.0.0");
        assertThat(Files.readString(artifactDirectory.resolve("sample-tool-1.0.0.pom")))
                .isEqualTo(publishedPom);
        assertThat(Files.readAllBytes(artifactDirectory.resolve("sample-tool-1.0.0.jar")))
                .isEqualTo(Files.readAllBytes(jar));
        assertThat(Files.readString(bundlePom)).contains("<artifactId>sample-tool</artifactId>");
        assertThat(result.record().source()).containsEntry("mavenPomSource", "embedded");
    }

    @Test
    void rejectsDifferentContentForTheSameCoordinatesButAllowsANewVersion() throws Exception {
        Path localRoot = temporaryDirectory.resolve("tools");
        Path repository = temporaryDirectory.resolve("repository/artifacts");
        Path bundlePom = writeBundlePom();
        MavenCoordinatesSpec coordinates = coordinates("1.0.0");
        Path firstJar = jarWithPom(localRoot, "first.jar", publishedPom(coordinates, null, null, null), "example/Tool.class");
        Path changedJar = jarWithPom(localRoot, "changed.jar", publishedPom(coordinates, null, null, null), "example/Changed.class");
        BundleToolRegistrationStrategy strategy = strategy(localRoot, repository, bundlePom);

        strategy.register(request(firstJar, coordinates), context());

        assertThatThrownBy(() -> strategy.register(request(changedJar, coordinates), context()))
                .isInstanceOf(JsonRpcException.class)
                .hasMessageContaining("different JAR")
                .satisfies(exception -> assertThat(((JsonRpcException) exception).data().path("errorCode").asString())
                        .isEqualTo("BUNDLE_MAVEN_COORDINATE_CONFLICT"));

        MavenCoordinatesSpec nextVersion = coordinates("1.0.1");
        Path nextJar = jarWithPom(localRoot, "next.jar", publishedPom(nextVersion, null, null, null), "example/Next.class");
        strategy.register(request(nextJar, nextVersion), context());

        assertThat(Files.isRegularFile(repository.resolve("dev/mrk/tools/sample-tool/1.0.1/sample-tool-1.0.1.jar")))
                .isTrue();
    }

    @Test
    void rejectsReservedMavenGroupsAndNativeMeshingressPackages() throws Exception {
        Path localRoot = temporaryDirectory.resolve("tools");
        Path repository = temporaryDirectory.resolve("repository/artifacts");
        Path bundlePom = writeBundlePom();
        BundleToolRegistrationStrategy strategy = strategy(localRoot, repository, bundlePom);

        MavenCoordinatesSpec reservedGroup = new MavenCoordinatesSpec("org.meshingress.plugin", "sample-tool", "1.0.0", List.of());
        Path harmlessJar = jarWithPom(localRoot, "harmless.jar", publishedPom(reservedGroup, null, null, null), "example/Tool.class");
        assertThatThrownBy(() -> strategy.register(request(harmlessJar, reservedGroup), context()))
                .isInstanceOf(JsonRpcException.class)
                .hasMessageContaining("reserved");

        MavenCoordinatesSpec allowedGroup = coordinates("1.0.0");
        Path nativeJar = jarWithPom(localRoot, "native.jar", publishedPom(allowedGroup, null, null, null), "dev/mrk/meshingress/Override.class");
        assertThatThrownBy(() -> strategy.register(request(nativeJar, allowedGroup), context()))
                .isInstanceOf(JsonRpcException.class)
                .hasMessageContaining("reserved native Meshingress package");
    }

    private BundleToolRegistrationStrategy strategy(Path localRoot, Path repository, Path bundlePom) {
        ToolRegistry registry = mock(ToolRegistry.class);
        when(registry.findTool("sample.tool")).thenReturn(Optional.empty());
        when(registry.findEnabledFunction("sample.tool")).thenReturn(Optional.empty());
        return new BundleToolRegistrationStrategy(
                properties(localRoot, bundlePom),
                new ToolArtifactResolutionContext(repository, null, false, List.of()),
                registry,
                new InMemoryToolRegistrationStore()
        );
    }

    private ToolRegistrationRequest request(Path jar, MavenCoordinatesSpec coordinates) throws Exception {
        return new ToolRegistrationRequest(
                "sample.tool",
                ToolRegistrationPhase.BUNDLE,
                new LocalJarSpec(jar.getFileName().toString(), sha256(jar), null),
                coordinates,
                new BundleSpec("meshingress-tool-bundle"),
                null,
                false
        );
    }

    private ToolRegistrationContext context() {
        return new ToolRegistrationContext(new McpCallContext("Bearer dev-admin", null, null, "request-1"), "test", OffsetDateTime.now());
    }

    private Path writeBundlePom() throws Exception {
        Path pom = temporaryDirectory.resolve("bundle/pom.xml");
        Files.createDirectories(pom.getParent());
        Files.writeString(pom, "<project><dependencies></dependencies></project>");
        return pom;
    }

    private Path jarWithPom(Path localRoot, String name, String pom, String classEntry) throws Exception {
        Files.createDirectories(localRoot);
        Path jar = localRoot.resolve(name);
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(classEntry));
            output.write(new byte[] {0});
            output.closeEntry();
            output.putNextEntry(new JarEntry("META-INF/maven/dev.mrk.tools/sample-tool/pom.xml"));
            output.write(pom.getBytes());
            output.closeEntry();
        }
        return jar;
    }

    private MavenCoordinatesSpec coordinates(String version) {
        return new MavenCoordinatesSpec("dev.mrk.tools", "sample-tool", version, List.of());
    }

    private String publishedPom(MavenCoordinatesSpec coordinates, String dependencyGroupId, String dependencyArtifactId, String dependencyVersion) {
        String dependencies = dependencyGroupId == null ? "" : """
                <dependencies>
                  <dependency>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                  </dependency>
                </dependencies>
                """.formatted(dependencyGroupId, dependencyArtifactId, dependencyVersion);
        return """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                  %s
                </project>
                """.formatted(coordinates.groupId(), coordinates.artifactId(), coordinates.version(), dependencies);
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private MeshingressProperties properties(Path localRoot, Path bundlePom) {
        MeshingressProperties defaults = new MeshingressProperties(null, null, null, null, null, null, null, null, null, null, null);
        MeshingressProperties.Tools defaultsTools = defaults.tools();
        MeshingressProperties.Tools.Registration registration = defaultsTools.registration();
        MeshingressProperties.Tools.Registration configuredRegistration = new MeshingressProperties.Tools.Registration(
                registration.enabled(),
                registration.allowExperimental(),
                registration.allowStaging(),
                registration.allowBundle(),
                registration.allowNativeHttp(),
                registration.experimentalReplaceExisting(),
                registration.allowExperimentalOverrideBundle(),
                registration.allowExperimentalOverrideStaging(),
                registration.allowStagingOverrideBundle(),
                registration.allowOverrideNative(),
                registration.stagingConflictPolicy(),
                localRoot.toString(),
                bundlePom.toString(),
                registration.requireLocalJarChecksum(),
                registration.requireMavenVersionPin(),
                registration.requireApprovalForDynamicPhases()
        );
        MeshingressProperties.Tools tools = new MeshingressProperties.Tools(
                defaultsTools.registry(),
                configuredRegistration,
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
                defaults.repository(),
                defaults.storage()
        );
    }
}
