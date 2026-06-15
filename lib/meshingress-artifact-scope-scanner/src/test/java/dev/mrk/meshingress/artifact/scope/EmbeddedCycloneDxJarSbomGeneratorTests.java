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

    private static List<String> componentNames(JsonNode root) {
        List<String> names = new ArrayList<>();
        for (JsonNode component : root.path("components")) {
            names.add(component.path("name").asString());
        }
        return names;
    }

    private static void writeJar(Path jar, Class<?>... classes) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            writeEntry(zip, "META-INF/meshingress/descriptor.json", "{\"name\":\"representative\"}".getBytes(StandardCharsets.UTF_8));
            for (Class<?> type : classes) {
                writeEntry(zip, entryName(type), classBytes(type));
            }
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
