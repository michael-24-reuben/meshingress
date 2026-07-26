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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.repository.publication-eligibility.trusted-signing-key-ids=trusted-publication-key",
        "meshingress.repository.publication-eligibility.revoked-signing-key-ids=local-dev-hmac"
})
@AutoConfigureMockMvc
class ArtifactRepositorySigningKeyEligibilityTests {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("repository/sql/signing-key-eligibility"));
    }

    @Test
    void publicationPolicyRejectsUnknownAndRevokedSigningKey() throws Exception {
        uploadAssessAndApprove("signing-key-sample");

        mockMvc.perform(post("/api/v1/artifact/dev.mrk.tools/signing-key-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("SIGNING_KEY_UNKNOWN:local-dev-hmac")))
                .andExpect(jsonPath("$.detail", containsString("SIGNING_KEY_REVOKED:local-dev-hmac")));
    }

    private void uploadAssessAndApprove(String artifactId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                artifactId + ".jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/api/v1/artifact/dev.mrk.tools/" + artifactId + "/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/artifact/dev.mrk.tools/" + artifactId + "/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/artifact/dev.mrk.tools/" + artifactId + "/1.0.0/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["FILES_READ"],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "test"
                                }
                                """)
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk());
    }

    private byte[] sampleJarBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
