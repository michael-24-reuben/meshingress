package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.security.Finding;
import dev.mrk.meshingress.artifact.security.ScannerAdapter;
import dev.mrk.meshingress.artifact.security.ScannerRequest;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import dev.mrk.meshingress.artifact.security.ScannerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.repository.scanner-pipeline.required-scanners=policy-blocking-scanner,cyclonedx-sbom,embedded-jar-sandbox,bytecode-scope-scanner",
        "meshingress.repository.scanner-pipeline.scanner-versions.policy-blocking-scanner=1.0-test",
        "meshingress.repository.scanner-pipeline.scanner-failure-policies.policy-blocking-scanner=block",
        "meshingress.repository.scanner-pipeline.scanner-timeouts.policy-blocking-scanner=5s"
})
@AutoConfigureMockMvc
class ArtifactRepositoryScannerPipelineTests {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("repository/sql/scanner-pipeline"));
    }

    @Test
    void blockedRequiredScannerPreventsApprovalAndPublication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blocked-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/blocked-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("QUARANTINED"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/blocked-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("BLOCKED_POLICY"))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("policy-blocking-scanner")))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("embedded-jar-sandbox")))
                .andExpect(jsonPath("$.assessment.summary.pipeline.requiredScanners", hasItem("policy-blocking-scanner")));

        mockMvc.perform(get("/artifact/dev.mrk.tools/blocked-sample/1.0.0/assessment")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanner").value("policy-blocking-scanner"))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.required").value(true))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.expectedVersion").value("1.0-test"))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.timeout").value("PT5S"))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.failurePolicy").value("BLOCK"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/blocked-sample/1.0.0/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["FILES_READ"],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "test"
                                }
                                """)
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be REVIEW_PENDING before approval"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/blocked-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be approved before publication"));
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

    @TestConfiguration
    static class ScannerPipelineTestConfiguration {

        @Bean
        ScannerAdapter policyBlockingScanner() {
            return new ScannerAdapter() {
                @Override
                public String name() {
                    return "policy-blocking-scanner";
                }

                @Override
                public ScannerResult scan(ScannerRequest request) {
                    return new ScannerResult(
                            name(),
                            "1.0-test",
                            ScannerStatus.BLOCKED,
                            List.of(new Finding(
                                    "HIGH",
                                    "TEST_BLOCK",
                                    "Required test scanner blocked this artifact.",
                                    request.artifactPath().getFileName().toString()
                            )),
                            Map.of("mode", "test"),
                            null
                    );
                }
            };
        }
    }
}
