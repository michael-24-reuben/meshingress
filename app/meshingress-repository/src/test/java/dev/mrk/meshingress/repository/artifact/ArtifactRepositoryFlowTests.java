package dev.mrk.meshingress.repository.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArtifactRepositoryFlowTests {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
    }

    @Test
    void uploadAssessApproveAndPublishArtifactWithFakeScanner() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "generated-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/generated-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "SHELL_EXECUTE")
                        .param("requestedScopes", "FILES_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("QUARANTINED"))
                .andExpect(jsonPath("$.artifactChecksum.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.files[0].path").exists());

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/assess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVIEW_PENDING"))
                .andExpect(jsonPath("$.assessment.scanners[0]").value("fake-scanner"))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("bytecode-scope-scanner")))
                .andExpect(jsonPath("$.scopes.inferredScopes", hasItem("FILES_READ")));

        mockMvc.perform(get("/artifact/dev.mrk.tools/generated-sample/1.0.0/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanner").value("fake-scanner"))
                .andExpect(jsonPath("$[0].status").value("PASSED"))
                .andExpect(jsonPath("$[1].scanner").value("bytecode-scope-scanner"))
                .andExpect(jsonPath("$[1].status").value("REVIEW"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["FILES_READ"],
                                  "deniedScopes": [
                                    {
                                      "scope": "SHELL_EXECUTE",
                                      "reason": "Not approved for the first repository smoke flow."
                                    }
                                  ],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.scopes.approvedScopes[0]").value("FILES_READ"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"))
                .andExpect(jsonPath("$.signature", not(blankOrNullString())));

        Path root = tempDir.resolve("repository");
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("metadata/dev/mrk/tools/generated-sample/1.0.0/record.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("assessments/dev/mrk/tools/generated-sample/1.0.0/assessment.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("publications/dev/mrk/tools/generated-sample/1.0.0/publication.json"))).isTrue();
    }

    private byte[] sampleJarBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("bin/run.ps1"));
            zip.write("Write-Output 'sample'\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            String fixtureClass = FileReadFixture.class.getName().replace('.', '/') + ".class";
            zip.putNextEntry(new ZipEntry(fixtureClass));
            zip.write(classBytes(FileReadFixture.class));
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

    private static final class FileReadFixture {
        String read(Path path) throws IOException {
            return Files.readString(path);
        }
    }
}
