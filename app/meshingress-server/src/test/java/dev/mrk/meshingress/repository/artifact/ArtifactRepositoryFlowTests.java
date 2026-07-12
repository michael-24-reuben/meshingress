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
import tools.jackson.databind.JsonNode;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        byte[] artifactBytes = sampleJarBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "generated-sample.jar",
                "application/java-archive",
                artifactBytes
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

        mockMvc.perform(get("/artifact/dev.mrk.tools/generated-sample/1.0.0/file")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(artifactBytes));

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer")
                        .header("X-Repository-Actor", "review-agent")
                        .header("X-Request-Id", "req-assess-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVIEW_PENDING"))
                .andExpect(jsonPath("$.assessment.scanners", not(hasItem("fake-scanner"))))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("cyclonedx-sbom")))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("embedded-jar-sandbox")))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("bytecode-scope-scanner")))
                .andExpect(jsonPath("$.assessment.summary.sbom.format").value("CycloneDX"))
                .andExpect(jsonPath("$.assessment.summary.sbom.componentCount").value(7))
                .andExpect(jsonPath("$.assessment.summary.sbom.dependencyComponentCount").value(1))
                .andExpect(jsonPath("$.assessment.summary.pipeline.requiredScanners", hasItem("cyclonedx-sbom")))
                .andExpect(jsonPath("$.assessment.summary.pipeline.requiredScanners", hasItem("embedded-jar-sandbox")))
                .andExpect(jsonPath("$.assessment.summary.pipeline.requiredScanners", hasItem("bytecode-scope-scanner")))
                .andExpect(jsonPath("$.assessment.summary.rawReportRetention.policy").value("retain-with-artifact"))
                .andExpect(jsonPath("$.assessment.summary.sandbox.strategy").value("static-quarantine-inspection"))
                .andExpect(jsonPath("$.scopes.inferredScopes", hasItem("FILES_READ")));

        mockMvc.perform(get("/artifact/dev.mrk.tools/generated-sample/1.0.0/assessment")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanner").value("cyclonedx-sbom"))
                .andExpect(jsonPath("$[0].status").value("PASSED"))
                .andExpect(jsonPath("$[0].rawSummary.rawReport").value("cyclonedx-sbom.json"))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.required").value(true))
                .andExpect(jsonPath("$[0].rawSummary.pipeline.failurePolicy").value("BLOCK"))
                .andExpect(jsonPath("$[0].rawReportPath").exists())
                .andExpect(jsonPath("$[1].scanner").value("embedded-jar-sandbox"))
                .andExpect(jsonPath("$[1].status").value("REVIEW"))
                .andExpect(jsonPath("$[1].rawSummary.rawReport").value("embedded-jar-sandbox.json"))
                .andExpect(jsonPath("$[1].rawSummary.isolation.networkAccess").value(false))
                .andExpect(jsonPath("$[1].rawSummary.isolation.hostSecretsAccess").value(false))
                .andExpect(jsonPath("$[1].rawReportPath").exists())
                .andExpect(jsonPath("$[2].scanner").value("bytecode-scope-scanner"))
                .andExpect(jsonPath("$[2].status").value("PASSED"));

        mockMvc.perform(get("/artifact/reviews/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to read"));

        mockMvc.perform(get("/artifact/reviews/pending")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].artifact.coordinate.groupId").value("dev.mrk.tools"))
                .andExpect(jsonPath("$[0].artifact.coordinate.artifactId").value("generated-sample"))
                .andExpect(jsonPath("$[0].artifact.trustStatus").value("REVIEW_PENDING"))
                .andExpect(jsonPath("$[0].artifact.scopes.requestedScopes", hasItem("SHELL_EXECUTE")))
                .andExpect(jsonPath("$[0].artifact.scopes.inferredScopes", hasItem("FILES_READ")))
                .andExpect(jsonPath("$[0].artifact.scopes.approvedScopes").isEmpty())
                .andExpect(jsonPath("$[0].artifact.scopes.deniedScopes").isEmpty())
                .andExpect(jsonPath("$[0].assessment[0].scanner").value("cyclonedx-sbom"))
                .andExpect(jsonPath("$[0].assessment[1].scanner").value("embedded-jar-sandbox"))
                .andExpect(jsonPath("$[0].assessment[2].scanner").value("bytecode-scope-scanner"));

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

        mockMvc.perform(get("/artifact/reviews/pending")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.artifact.coordinate.artifactId == 'generated-sample')]").isEmpty());

        mockMvc.perform(post("/artifact/dev.mrk.tools/generated-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher")
                        .header("X-Repository-Actor", "publisher-agent")
                        .header("X-Request-Id", "req-publish-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureKeyId").value("local-dev-hmac"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"))
                .andExpect(jsonPath("$.provenance.generatedBy").value("meshingress-repository"))
                .andExpect(jsonPath("$.eligibilityDecision.eligible").value(true))
                .andExpect(jsonPath("$.eligibilityDecision.evidence.requiredScanners", hasItem("cyclonedx-sbom")))
                .andExpect(jsonPath("$.eligibilityDecision.evidence.presentScanners", hasItem("embedded-jar-sandbox")))
                .andExpect(jsonPath("$.eligibilityDecision.evidence.reviewerApprovalCount").value(1))
                .andExpect(jsonPath("$.signature", not(blankOrNullString())));

        mockMvc.perform(get("/artifact/dev.mrk.tools/generated-sample/1.0.0/publication")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureKeyId").value("local-dev-hmac"))
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
        org.assertj.core.api.Assertions.assertThat(reloadedStore.findAssessment(coordinate)).hasSize(3);
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
        Path artifactDirectory = root.resolve("artifacts/dev/mrk/tools/generated-sample/1.0.0");
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("metadata/dev/mrk/tools/generated-sample/1.0.0/record.json"))).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("reviews/dev/mrk/tools/generated-sample/1.0.0/latest-review.json"))).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(artifactDirectory.resolve("assessment.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(artifactDirectory.resolve("cyclonedx-sbom.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(artifactDirectory.resolve("embedded-jar-sandbox.json"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("resources/application.yaml")))
                .contains("meshingress.sample.file-read.root: \"./var/meshingress/sample\"")
                .contains("Default root used by the sample tool when reading files.");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("README.md")))
                .contains("# Generated Sample Tool")
                .contains("README content exported from native tool metadata.");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("resources/README.md")))
                .contains("# Generated Sample Tool")
                .contains("README content exported from native tool metadata.");
        org.assertj.core.api.Assertions.assertThat(Files.readString(artifactDirectory.resolve("resources/tool-manifest.json")))
                .contains("\"toolId\":\"generated.sample\"")
                .contains("\"meshingress.sample.file-read.root\"");
        JsonNode rawSbom = objectMapper.readTree(Files.readString(artifactDirectory.resolve("cyclonedx-sbom.json")));
        org.assertj.core.api.Assertions.assertThat(rawSbom.path("metadata").path("component").path("purl").asString())
                .isEqualTo("pkg:maven/dev.mrk.tools/generated-sample@1.0.0");
        org.assertj.core.api.Assertions.assertThat(componentNamed(rawSbom, "jackson-databind").path("purl").asString())
                .isEqualTo("pkg:maven/tools.jackson.core/jackson-databind@3.2.0");
        JsonNode rawSandbox = objectMapper.readTree(Files.readString(artifactDirectory.resolve("embedded-jar-sandbox.json")));
        org.assertj.core.api.Assertions.assertThat(rawSandbox.path("strategy").asString()).isEqualTo("static-quarantine-inspection");
        org.assertj.core.api.Assertions.assertThat(rawSandbox.path("isolation").path("networkAccess").asBoolean()).isFalse();
        org.assertj.core.api.Assertions.assertThat(rawSandbox.path("isolation").path("hostSecretsAccess").asBoolean()).isFalse();
        org.assertj.core.api.Assertions.assertThat(rawSandbox.path("retention").path("policy").asString()).isEqualTo("retain-with-artifact");
        org.assertj.core.api.Assertions.assertThat(Files.exists(root.resolve("assessments/dev/mrk/tools/generated-sample/1.0.0"))).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.exists(root.resolve("quarantine/dev/mrk/tools/generated-sample/1.0.0"))).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(root.resolve("publications/dev/mrk/tools/generated-sample/1.0.0/publication.json"))).isFalse();
    }

    @Test
    void listsUploadedJarMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "listed-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/listed-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader")
                        .header("X-Repository-Actor", "upload-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("QUARANTINED"));

        mockMvc.perform(get("/artifact/jars"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to read"));

        mockMvc.perform(get("/artifact/jars")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')].coordinate.groupId", hasItem("dev.mrk.tools")))
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')].coordinate.packaging", hasItem("jar")))
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')].trustStatus", hasItem("QUARANTINED")))
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')].artifactChecksum.algorithm", hasItem("SHA-256")))
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')].scopes.requestedScopes[0]", hasItem("FILES_READ")))
                .andExpect(jsonPath("$[?(@.coordinate.artifactId == 'listed-sample')].files[0].path").isNotEmpty());
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

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "test",
                                  "notes": "Reject cannot bypass assessment."
                                }
                                """)
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be assessed before rejection"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be assessed before publication"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "test",
                                  "notes": "Revoke cannot bypass publication."
                                }
                                """)
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be published before revocation"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/stage-bypass-sample/1.0.0/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "test",
                                  "notes": "Restore cannot bypass delete."
                                }
                                """)
                        .header("X-Repository-Role", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be DELETED before restore"));

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
    void reviewerCanRejectPendingArtifactWithLifecycleAudit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rejected-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/rejected-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "SHELL_EXECUTE")
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/rejected-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVIEW_PENDING"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/rejected-sample/1.0.0/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deniedScopes": [
                                    {
                                      "scope": "SHELL_EXECUTE",
                                      "reason": "Shell access is not approved for this artifact."
                                    }
                                  ],
                                  "reviewer": "test-reviewer",
                                  "notes": "Rejecting unsafe requested scope."
                                }
                                """)
                        .header("X-Repository-Role", "reviewer")
                        .header("X-Repository-Actor", "human-reviewer")
                        .header("X-Request-Id", "req-reject-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REJECTED"))
                .andExpect(jsonPath("$.scopes.requestedScopes", hasItem("SHELL_EXECUTE")))
                .andExpect(jsonPath("$.scopes.inferredScopes", hasItem("FILES_READ")))
                .andExpect(jsonPath("$.scopes.approvedScopes").isEmpty())
                .andExpect(jsonPath("$.scopes.deniedScopes[0].scope").value("SHELL_EXECUTE"));

        mockMvc.perform(get("/artifact/reviews/pending")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.artifact.coordinate.artifactId == 'rejected-sample')]").isEmpty());

        mockMvc.perform(post("/artifact/dev.mrk.tools/rejected-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("approved before publication")));

        String lifecycleEventsTable = repositoryProperties.sql().schema() + "." + repositoryProperties.sql().table().lifecycleEvents();
        Integer rejectAuditEvents = jdbcTemplate.queryForObject(
                "select count(*) from " + lifecycleEventsTable + " where event_type = ? and actor = ? and request_id = ? and from_state = ? and to_state = ? and created_at is not null",
                Integer.class,
                "REJECT",
                "human-reviewer",
                "req-reject-1",
                "REVIEW_PENDING",
                "REJECTED"
        );
        org.assertj.core.api.Assertions.assertThat(rejectAuditEvents).isEqualTo(1);
    }

    @Test
    void publisherCanRevokePublishedArtifactWithLifecycleAudit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "revoked-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/revoked-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/revoked-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/revoked-sample/1.0.0/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["FILES_READ"],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "test"
                                }
                                """)
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/revoked-sample/1.0.0/publish")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(false))
                .andExpect(jsonPath("$.signature", not(blankOrNullString())));

        mockMvc.perform(post("/artifact/dev.mrk.tools/revoked-sample/1.0.0/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "repository-admin",
                                  "notes": "Delete should require revoke first."
                                }
                                """)
                        .header("X-Repository-Role", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("published artifacts must be revoked before deletion"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/revoked-sample/1.0.0/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "publisher-agent",
                                  "notes": "Revoking after publication."
                                }
                                """)
                        .header("X-Repository-Role", "publisher")
                        .header("X-Repository-Actor", "publisher-agent")
                        .header("X-Request-Id", "req-revoke-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVOKED"))
                .andExpect(jsonPath("$.revoked").value(true))
                .andExpect(jsonPath("$.signatureKeyId").value("local-dev-hmac"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"))
                .andExpect(jsonPath("$.signature", not(blankOrNullString())));

        mockMvc.perform(get("/artifact/dev.mrk.tools/revoked-sample/1.0.0/metadata")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVOKED"));

        mockMvc.perform(get("/artifact/dev.mrk.tools/revoked-sample/1.0.0/publication")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVOKED"))
                .andExpect(jsonPath("$.revoked").value(true));

        mockMvc.perform(post("/artifact/dev.mrk.tools/revoked-sample/1.0.0/revoke")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("publication is already revoked"));

        ArtifactCoordinate coordinate = new ArtifactCoordinate("dev.mrk.tools", "revoked-sample", "1.0.0", null, "jar");
        SqlArtifactMetadataStore reloadedStore = new SqlArtifactMetadataStore(
                jdbcTemplate,
                repositoryProperties.sql(),
                objectMapper
        );
        org.assertj.core.api.Assertions.assertThat(reloadedStore.findArtifact(coordinate).orElseThrow().record().trustStatus().name()).isEqualTo("REVOKED");
        org.assertj.core.api.Assertions.assertThat(reloadedStore.findPublication(coordinate).orElseThrow().revoked()).isTrue();

        String lifecycleEventsTable = repositoryProperties.sql().schema() + "." + repositoryProperties.sql().table().lifecycleEvents();
        Integer revokeAuditEvents = jdbcTemplate.queryForObject(
                "select count(*) from " + lifecycleEventsTable + " where event_type = ? and actor = ? and request_id = ? and from_state = ? and to_state = ? and created_at is not null",
                Integer.class,
                "REVOKE",
                "publisher-agent",
                "req-revoke-1",
                "APPROVED_LIMITED",
                "REVOKED"
        );
        org.assertj.core.api.Assertions.assertThat(revokeAuditEvents).isEqualTo(1);
    }

    @Test
    void adminCanSoftDeleteAndRestoreArtifactWithLifecycleAudit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "deleted-sample.jar",
                "application/java-archive",
                sampleJarBytes()
        );

        mockMvc.perform(multipart("/artifact/dev.mrk.tools/deleted-sample/1.0.0")
                        .file(file)
                        .param("requestedScopes", "FILES_READ")
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/deleted-sample/1.0.0/assess")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/artifact/dev.mrk.tools/deleted-sample/1.0.0/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["FILES_READ"],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "test"
                                }
                                """)
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/deleted-sample/1.0.0/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "repository-admin",
                                  "notes": "Soft delete for repository lifecycle testing."
                                }
                                """)
                        .header("X-Repository-Role", "admin")
                        .header("X-Repository-Actor", "repository-admin")
                        .header("X-Request-Id", "req-delete-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("DELETED"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/deleted-sample/1.0.0/delete")
                        .header("X-Repository-Role", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact is already deleted"));

        ArtifactCoordinate coordinate = new ArtifactCoordinate("dev.mrk.tools", "deleted-sample", "1.0.0", null, "jar");
        SqlArtifactMetadataStore reloadedStore = new SqlArtifactMetadataStore(
                jdbcTemplate,
                repositoryProperties.sql(),
                objectMapper
        );
        ArtifactMetadataEntry deletedEntry = reloadedStore.findArtifact(coordinate).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deletedEntry.record().trustStatus().name()).isEqualTo("DELETED");
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(deletedEntry.artifactPath())).isTrue();

        mockMvc.perform(post("/artifact/dev.mrk.tools/deleted-sample/1.0.0/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "repository-admin",
                                  "notes": "Restoring soft-deleted artifact."
                                }
                                """)
                        .header("X-Repository-Role", "admin")
                        .header("X-Repository-Actor", "repository-admin")
                        .header("X-Request-Id", "req-restore-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/deleted-sample/1.0.0/restore")
                        .header("X-Repository-Role", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("artifact must be DELETED before restore"));

        ArtifactMetadataEntry restoredEntry = reloadedStore.findArtifact(coordinate).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(restoredEntry.record().trustStatus().name()).isEqualTo("APPROVED_LIMITED");
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(restoredEntry.artifactPath())).isTrue();

        String lifecycleEventsTable = repositoryProperties.sql().schema() + "." + repositoryProperties.sql().table().lifecycleEvents();
        Integer deleteAuditEvents = jdbcTemplate.queryForObject(
                "select count(*) from " + lifecycleEventsTable + " where event_type = ? and actor = ? and request_id = ? and from_state = ? and to_state = ? and created_at is not null",
                Integer.class,
                "DELETE",
                "repository-admin",
                "req-delete-1",
                "APPROVED_LIMITED",
                "DELETED"
        );
        org.assertj.core.api.Assertions.assertThat(deleteAuditEvents).isEqualTo(1);
        Integer restoreAuditEvents = jdbcTemplate.queryForObject(
                "select count(*) from " + lifecycleEventsTable + " where event_type = ? and actor = ? and request_id = ? and from_state = ? and to_state = ? and created_at is not null",
                Integer.class,
                "RESTORE",
                "repository-admin",
                "req-restore-1",
                "DELETED",
                "APPROVED_LIMITED"
        );
        org.assertj.core.api.Assertions.assertThat(restoreAuditEvents).isEqualTo(1);
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

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "test"
                                }
                                """)
                        .header("X-Repository-Role", "uploader"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to reject"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/publish")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to publish"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/revoke")
                        .header("X-Repository-Role", "reviewer"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to revoke"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/delete")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to delete"));

        mockMvc.perform(post("/artifact/dev.mrk.tools/role-gated-sample/1.0.0/restore")
                        .header("X-Repository-Role", "publisher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("repository role is not allowed to restore"));
    }

    private byte[] sampleJarBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("META-INF/meshingress/tool-manifest.json"));
            zip.write(generatedSampleManifestJson().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("bin/run.ps1"));
            zip.write("Write-Output 'sample'\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("META-INF/maven/dev.mrk.tools/generated-sample/pom.properties"));
            zip.write("""
                    groupId=dev.mrk.tools
                    artifactId=generated-sample
                    version=1.0.0
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("META-INF/maven/dev.mrk.tools/generated-sample/pom.xml"));
            zip.write("""
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>dev.mrk.tools</groupId>
                      <artifactId>generated-sample</artifactId>
                      <version>1.0.0</version>
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

    private String generatedSampleManifestJson() {
        return """
                {
                  "schemaVersion": 1,
                  "toolId": "generated.sample",
                  "properties": [
                    {
                      "name": "meshingress.sample.file-read.root",
                      "description": "Default root used by the sample tool when reading files.",
                      "defaultValue": "./var/meshingress/sample",
                      "valueType": "string",
                      "required": false,
                      "secret": false
                    }
                  ],
                  "requirements": [
                    {
                      "kind": "scope",
                      "name": "FILES_READ",
                      "description": "Read files from the local filesystem.",
                      "required": true,
                      "label": "",
                      "license": "",
                      "vcs": "",
                      "cloneUrl": "",
                      "checkoutRef": "",
                      "baseUrlProperty": "",
                      "credentialProperty": "",
                      "scope": "FILES_READ",
                      "canonicalIdentity": ""
                    }
                  ],
                  "links": [],
                  "readme": "# Generated Sample Tool\\n\\nREADME content exported from native tool metadata."
                }
                """;
    }

    private JsonNode componentNamed(JsonNode root, String name) {
        for (JsonNode component : root.path("components")) {
            if (component.path("name").asString().equals(name)) {
                return component;
            }
        }
        throw new AssertionError("component not found: " + name);
    }

    private static final class FileReadFixture {
        String read(Path path) throws IOException {
            return Files.readString(path);
        }
    }
}
