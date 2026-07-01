package dev.mrk.meshingress.artifact.scope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedCycloneDxJarSbomGeneratorTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesDeterministicCycloneDxInventoryForRepresentativeJar(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("representative-tool.jar");
        writeJar(jar, RepresentativeTool.class, RepresentativeHelper.class);

        EmbeddedCycloneDxJarSbomGenerator generator = new EmbeddedCycloneDxJarSbomGenerator();

        CycloneDxSbom first = generator.generate(jar);
        CycloneDxSbom second = generator.generate(jar);
        String firstJson = first.toJson(objectMapper);
        String secondJson = second.toJson(objectMapper);
        JsonNode root = objectMapper.readTree(firstJson);

        assertThat(firstJson).isEqualTo(secondJson);
        assertThat(root.path("bomFormat").asString()).isEqualTo("CycloneDX");
        assertThat(root.path("specVersion").asString()).isEqualTo("1.6");
        assertThat(root.path("metadata").path("component").path("name").asString())
                .isEqualTo("representative-tool.jar");
        assertThat(root.path("metadata").path("component").path("hashes").get(0).path("content").asString())
                .hasSize(64);
        assertThat(root.path("components").size()).isEqualTo(3);

        List<String> componentNames = componentNames(root);
        assertThat(componentNames).isSorted();
        assertThat(componentNames).contains(
                "META-INF/meshingress/descriptor.json",
                entryName(RepresentativeTool.class),
                entryName(RepresentativeHelper.class)
        );

        assertThat(first.summary())
                .containsEntry("format", "CycloneDX")
                .containsEntry("specVersion", "1.6")
                .containsEntry("componentCount", 3)
                .containsEntry("artifactName", "representative-tool.jar");
        assertThat(first.summary().get("artifactSha256").toString()).hasSize(64);
    }

    @Test
    void includesMavenCoordinatesAndDeclaredDependenciesWhenPackagedMetadataExists(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("dependency-aware-1.2.3.jar");
        writeDependencyAwareJar(jar);

        CycloneDxSbom sbom = new EmbeddedCycloneDxJarSbomGenerator().generate(jar);
        JsonNode root = objectMapper.readTree(sbom.toJson(objectMapper));

        JsonNode metadataComponent = root.path("metadata").path("component");
        assertThat(metadataComponent.path("type").asString()).isEqualTo("application");
        assertThat(metadataComponent.path("group").asString()).isEqualTo("dev.mrk.tools");
        assertThat(metadataComponent.path("name").asString()).isEqualTo("dependency-aware");
        assertThat(metadataComponent.path("version").asString()).isEqualTo("1.2.3");
        assertThat(metadataComponent.path("purl").asString())
                .isEqualTo("pkg:maven/dev.mrk.tools/dependency-aware@1.2.3");

        assertThat(root.path("components").size()).isEqualTo(4);
        assertThat(sbom.summary())
                .containsEntry("componentCount", 4)
                .containsEntry("dependencyComponentCount", 1L)
                .containsEntry("artifactName", "dependency-aware");

        JsonNode dependency = findComponent(root, "jackson-databind");
        assertThat(dependency.path("type").asString()).isEqualTo("library");
        assertThat(dependency.path("group").asString()).isEqualTo("tools.jackson.core");
        assertThat(dependency.path("version").asString()).isEqualTo("3.2.0");
        assertThat(dependency.path("scope").asString()).isEqualTo("runtime");
        assertThat(dependency.path("purl").asString())
                .isEqualTo("pkg:maven/tools.jackson.core/jackson-databind@3.2.0");
    }

    private static List<String> componentNames(JsonNode root) {
        List<String> names = new ArrayList<>();
        for (JsonNode component : root.path("components")) {
            names.add(component.path("name").asString());
        }
        return names;
    }

    private static JsonNode findComponent(JsonNode root, String name) {
        for (JsonNode component : root.path("components")) {
            if (component.path("name").asString().equals(name)) {
                return component;
            }
        }
        throw new AssertionError("component not found: " + name);
    }

    private static void writeJar(Path jar, Class<?>... classes) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            writeEntry(zip, "META-INF/meshingress/descriptor.json", "{\"name\":\"representative\"}".getBytes(StandardCharsets.UTF_8));
            for (Class<?> type : classes) {
                writeEntry(zip, entryName(type), classBytes(type));
            }
        }
    }

    private static void writeDependencyAwareJar(Path jar) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            writeEntry(zip, "META-INF/maven/dev.mrk.tools/dependency-aware/pom.properties", """
                    groupId=dev.mrk.tools
                    artifactId=dependency-aware
                    version=1.2.3
                    """.getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, "META-INF/maven/dev.mrk.tools/dependency-aware/pom.xml", """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>dev.mrk.tools</groupId>
                      <artifactId>dependency-aware</artifactId>
                      <version>1.2.3</version>
                      <dependencies>
                        <dependency>
                          <groupId>tools.jackson.core</groupId>
                          <artifactId>jackson-databind</artifactId>
                          <version>3.2.0</version>
                          <scope>runtime</scope>
                        </dependency>
                      </dependencies>
                    </project>
                    """.getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, entryName(RepresentativeTool.class), classBytes(RepresentativeTool.class));
        }
    }

    private static void writeEntry(ZipOutputStream zip, String entryName, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + entryName(type);
        try (InputStream inputStream = type.getResourceAsStream(resourceName)) {
            assertThat(inputStream).isNotNull();
            return inputStream.readAllBytes();
        }
    }

    private static String entryName(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }

    private static final class RepresentativeTool {
        String call() {
            return new RepresentativeHelper().message();
        }
    }

    private static final class RepresentativeHelper {
        String message() {
            return "representative";
        }
    }
}
