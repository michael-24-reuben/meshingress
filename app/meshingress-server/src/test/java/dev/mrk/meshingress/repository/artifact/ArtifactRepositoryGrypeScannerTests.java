package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.security.ScannerProcessRequest;
import dev.mrk.meshingress.artifact.security.ScannerProcessResult;
import dev.mrk.meshingress.artifact.security.ScannerProcessRunner;
import dev.mrk.meshingress.artifact.security.ScannerProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.repository.scanner-pipeline.required-scanners=grype-vulnerability-scanner,cyclonedx-sbom,embedded-jar-sandbox,bytecode-scope-scanner",
        "meshingress.repository.scanner-pipeline.scanner-versions.grype-vulnerability-scanner=0.115.0-test",
        "meshingress.repository.scanner-pipeline.scanner-timeouts.grype-vulnerability-scanner=7s",
        "meshingress.repository.scanner-pipeline.scanner-failure-policies.grype-vulnerability-scanner=block",
        "meshingress.repository.grype.executable=fake-grype",
        "meshingress.repository.grype.block-severity-threshold=critical",
        "meshingress.repository.grype.review-severity-threshold=low"
})
@AutoConfigureMockMvc
class ArtifactRepositoryGrypeScannerTests {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("repository/sql/grype-scanner"));
    }

    @Test
    void configuredGrypeScannerParticipatesInAssessment() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "grype-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/api/v1/artifact/dev.mrk.tools/grype-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/artifact/dev.mrk.tools/grype-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("BLOCKED_POLICY"))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("grype-vulnerability-scanner")));

        mockMvc.perform(get("/api/v1/artifact/dev.mrk.tools/grype-sample/1.0.0/assessment")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanner").value("grype-vulnerability-scanner"))
                .andExpect(jsonPath("$[0].scannerVersion").value("0.115.0-test"))
                .andExpect(jsonPath("$[0].status").value("BLOCKED"))
                .andExpect(jsonPath("$[0].findings[0].code").value("CVE-2026-0001"))
                .andExpect(jsonPath("$[0].rawSummary.rawReport").value("grype-vulnerability-scanner.json"))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.timeout").value("PT7S"));
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
    static class GrypeScannerTestConfiguration {

        @Bean
        @Primary
        ScannerProcessRunner testScannerProcessRunner() {
            return new ScannerProcessRunner() {
                @Override
                public ScannerProcessResult run(ScannerProcessRequest request) {
                    return new ScannerProcessResult(
                            request.scanner(),
                            request.command(),
                            request.workingDirectory(),
                            request.timeout(),
                            Duration.ofMillis(15),
                            ScannerProcessStatus.SUCCEEDED,
                            0,
                            grypeJson(),
                            "",
                            false,
                            false,
                            ""
                    );
                }
            };
        }

        private static String grypeJson() {
            return """
                    {
                      "matches": [
                        {
                          "vulnerability": {
                            "id": "CVE-2026-0001",
                            "severity": "Critical",
                            "description": "Repository wiring test vulnerability."
                          },
                          "artifact": {
                            "name": "log4j-core",
                            "version": "2.14.1",
                            "locations": [
                              { "path": "BOOT-INF/lib/log4j-core-2.14.1.jar" }
                            ]
                          }
                        }
                      ],
                      "source": { "type": "file" },
                      "descriptor": { "name": "grype-sample.jar" }
                    }
                    """;
        }
    }
}
