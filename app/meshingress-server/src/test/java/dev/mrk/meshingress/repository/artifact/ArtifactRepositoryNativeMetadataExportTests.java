package dev.mrk.meshingress.repository.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.repository.scope-scanner-enabled=false",
        "meshingress.repository.scanner-pipeline.required-scanners=cyclonedx-sbom,embedded-jar-sandbox",
        "meshingress.repository.publication-eligibility.enabled=false"
})
@AutoConfigureMockMvc
class ArtifactRepositoryNativeMetadataExportTests {

    @TempDir
    static Path tempDir;

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("repository/sql/native-metadata-store"));
    }

    @Test
    void assessExportsNativeApplicationPropertiesAndReadmeResourcesBesideArtifact() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "native-metadata-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/api/v1/artifact/dev.mrk.tools/native-metadata-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/artifact/dev.mrk.tools/native-metadata-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk());

        Path artifactDirectory = tempDir.resolve("repository/artifacts/dev/mrk/tools/native-metadata-sample/1.0.0");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("resources/application.properties")))
                .contains("meshingress.sample.native.command=sample-tool")
                .contains("Host command used by the native sample tool.");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("README.md")))
                .contains("# Native Metadata Sample")
                .contains("README content exported beside the assessed artifact.");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("resources/README.md")))
                .contains("# Native Metadata Sample")
                .contains("README content exported beside the assessed artifact.");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("resources/tool-manifest.json")))
                .contains("\"namespace\":\"sample\"")
                .contains("\"meshingress.sample.native.command\"");

        mockMvc.perform(get("/api/v1/artifact/dev.mrk.tools/native-metadata-sample/1.0.0/resources/application.properties")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("meshingress.sample.native.command=sample-tool")));
        mockMvc.perform(get("/api/v1/artifact/dev.mrk.tools/native-metadata-sample/1.0.0/resources/tool-manifest.json")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"namespace\":\"sample\"")));
        mockMvc.perform(get("/api/v1/artifact/dev.mrk.tools/native-metadata-sample/1.0.0/icon")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().string(containsString("Native sample icon")));
    }

    private byte[] sampleJarBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("META-INF/meshingress/tool-manifest.json"));
            zip.write(nativeManifestJson().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("icons/sample.svg"));
            zip.write("<svg xmlns=\"http://www.w3.org/2000/svg\"><title>Native sample icon</title></svg>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            String fixtureClass = NativeMetadataFixture.class.getName().replace('.', '/') + ".class";
            zip.putNextEntry(new ZipEntry(fixtureClass));
            zip.write(classBytes(NativeMetadataFixture.class));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream inputStream = type.getResourceAsStream(resourceName)) {
            org.assertj.core.api.Assertions.assertThat(inputStream).isNotNull();
            return inputStream.readAllBytes();
        }
    }

    private String nativeManifestJson() {
        return """
                {
                  "schemaVersion": 2,
                  "metadata": {
                    "namespace": "sample",
                    "title": "Native sample",
                    "summary": "",
                    "description": "",
                    "authors": [],
                    "license": "",
                    "tags": [],
                    "links": [],
                    "icon": {
                      "resourcePath": "icons/sample.svg",
                      "mimeType": "image/svg+xml",
                      "accessibleLabel": "Native sample icon"
                    }
                  },
                  "properties": [
                    {
                      "name": "meshingress.sample.native.command",
                      "description": "Host command used by the native sample tool.",
                      "defaultValue": "sample-tool",
                      "valueType": "string",
                      "required": false,
                      "secret": false
                    }
                  ],
                  "requirements": [],
                  "readme": { "markdown": "# Native Metadata Sample\\n\\nREADME content exported beside the assessed artifact." }
                }
                """;
    }

    private static final class NativeMetadataFixture {
    }
}
