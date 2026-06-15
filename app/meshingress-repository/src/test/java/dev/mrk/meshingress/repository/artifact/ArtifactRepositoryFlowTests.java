package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataEntry;
import dev.mrk.meshingress.repository.artifact.store.SqlArtifactMetadataStore;
import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
import static org.hamcrest.Matchers.containsString;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeshingressRepositoryProperties repositoryProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("repository/sql/test-metadata-store"));
    }

    @Test
    void uploadAssessApproveAndPublishArtifactWithRealAssessmentScanners() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "generated-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/generated-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "SHELL_EXECUTE")
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader")
                        .header("X-Repository-Actor", "upload-agent")
                        .header("X-Request-Id", "req-upload-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("QUARANTINED"))
                .andExpect(jsonPath("$.artifactChecksum.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.files[0].path").exists());

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer")
                        .header("X-Repository-Actor", "review-agent")
                        .header("X-Request-Id", "req-assess-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVIEW_PENDING"))
                .andExpect(jsonPath("$.assessment.scanners", not(hasItem("fake-scanner"))))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("cyclonedx-sbom")))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("bytecode-scope-scanner")))
                .andExpect(jsonPath("$.assessment.summary.sbom.format").value("CycloneDX"))
                .andExpect(jsonPath("$.assessment.summary.sbom.componentCount").value(3))
                .andExpect(jsonPath("$.scopes.inferredScopes", hasItem("FILES_READ")));

        mockMvc.perform(get("/artifact/dev.mrk.tools/generated-sample/1.0.0/assessment")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanner").value("cyclonedx-sbom"))
                .andExpect(jsonPath("$[0].status").value("PASSED"))
                .andExpect(jsonPath("$[0].rawSummary.rawReport").value("cyclonedx-sbom.json"))
                .andExpect(jsonPath("$[0].rawReportPath").exists())
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
                                """)
                        .header("X-Repository-Role", "reviewer")
                        .header("X-Repository-Actor", "human-reviewer")
                        .header("X-Request-Id", "req-approve-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.scopes.approvedScopes[0]").value("FILES_READ"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher")
                        .header("X-Repository-Actor", "publisher-agent")
                        .header("X-Request-Id", "req-publish-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"))
                .andExpect(jsonPath("$.signature", not(blankOrNullString())));

        mockMvc.perform(get("/artifact/dev.mrk.tools/generated-sample/1.0.0/publication")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"));

        ArtifactCoordinate coordinate = new ArtifactCoordinate("dev.mrk.tools", "generated-sample", "1.0.0", null, "jar");
        SqlArtifactMetadataStore reloadedStore = new SqlArtifactMetadataStore(
                jdbcTemplate,
                repositoryProperties.sql(),
                objectMapper
        );
        ArtifactMetadataEntry reloadedArtifact = reloadedStore.findArtifact(coordinate).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloadedArtifact.record().trustStatus().name()).isEqualTo("APPROVED_LIMITED");
        org.assertj.core.api.Assertions.assertThat(reloadedArtifact.record().artifactChecksum().algorithm()).isEqualTo("SHA-256");
        org.assertj.core.api.Assertions.assertThat(reloadedArtifact.record().files()).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(reloadedStore.findAssessment(coordinate)).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(reloadedStore.findPublication(coordinate)).isPresent();
        String lifecycleEventsTable = repositoryProperties.sql().schema() + "." + repositoryProperties.sql().table().lifecycleEvents();
        Integer actorEvents = jdbcTemplate.queryForObject(
                "select count(*) from " + lifecycleEventsTable + " where actor in (?, ?, ?, ?) and request_id in (?, ?, ?, ?)",
                Integer.class,
                "upload-agent",
                "review-agent",
                "human-reviewer",
                "publisher-agent",
                "req-upload-1",
                "req-assess-1",
                "req-approve-1",
                "req-publish-1"
        );
        org.assertj.core.api.Assertions.assertThat(actorEvents).isEqualTo(4);
        Integer approvalAuditEvents = jdbcTemplate.queryForObject(
                "select count(*) from " + lifecycleEventsTable + " where event_type = ? and actor = ? and request_id = ? and from_state = ? and to_state = ? and created_at is not null",
                Integer.class,
                "APPROVE",
                "human-reviewer",
                "req-approve-1",
                "REVIEW_PENDING",
                "APPROVED_LIMITED"
        );
        org.assertj.core.api.Assertions.assertThat(approvalAuditEvents).isEqualTo(1);

        Path root = tempDir.resolve("repository");
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("metadata/dev/mrk/tools/generated-sample/1.0.0/record.json"))).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("reviews/dev/mrk/tools/generated-sample/1.0.0/latest-review.json"))).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("assessments/dev/mrk/tools/generated-sample/1.0.0/assessment.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("assessments/dev/mrk/tools/generated-sample/1.0.0/cyclonedx-sbom.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("publications/dev/mrk/tools/generated-sample/1.0.0/publication.json"))).isFalse();
    }

    @Test
    void rejectsArtifactLifecycleStageBypassAttempts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "stage-bypass-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader")
                        .header("X-Repository-Actor", "upload-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("QUARANTINED"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/approve")
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
                .andExpect(jsonPath("$.detail").value("artifact must be assessed before approval"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be assessed before publication"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVIEW_PENDING"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("approved before publication")));
    }

    @Test
    void repositoryRolesBlockUnauthorizedReviewAndPublicationTransitions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "role-gated-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/role-gated-sample/1.0.0")
                        .file(file)
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["FILES_READ"],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "test"
                                }
                                """)
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to approve"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/publish")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to publish"));
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
