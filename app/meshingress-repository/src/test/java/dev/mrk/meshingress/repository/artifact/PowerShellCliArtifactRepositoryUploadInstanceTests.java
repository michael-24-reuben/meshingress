package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataEntry;
import dev.mrk.meshingress.repository.artifact.store.SqlArtifactMetadataStore;
import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PowerShellCliArtifactRepositoryUploadInstanceTests {

    private static final String GROUP_ID = "dev.mrk.toolspace";
    private static final String ARTIFACT_ID = "powershell-cli";
    private static final String VERSION = "0.0.1-SNAPSHOT";
    private static final String PACKAGING = "jar";
    private static final String JAR_NAME = "powershell-cli-0.0.1-SNAPSHOT.jar";

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
        registry.add("meshingress.repository.root", () -> repositoryRoot().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + repositoryRoot().resolve("sql/powershell-cli-upload-instance-test"));
    }

    @Test
    void uploadAssessReviewAndPublishPackagedPowerShellCliJarIntoRepository() throws Exception {
        Path jar = packagedJar();
        assumeTrue(jar != null, "Packaged PowerShell CLI jar not found under tools/lib: " + JAR_NAME);
        assertThat(Files.isRegularFile(jar)).isTrue();
        ArtifactCoordinate coordinate = new ArtifactCoordinate(GROUP_ID, ARTIFACT_ID, VERSION, null, PACKAGING);
        resetCoordinate(coordinate);

        System.out.println();
        System.out.println("=== PowerShell CLI repository upload/assessment smoke ===");
        System.out.println("packagedJar=" + jar.toAbsolutePath().normalize());
        System.out.println("packagedJarSize=" + Files.size(jar));
        System.out.println("repositoryRoot=" + repositoryRoot());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                jar.getFileName().toString(),
                "application/java-archive",
                Files.readAllBytes(jar)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/artifact/" + GROUP_ID + "/" + ARTIFACT_ID + "/" + VERSION)
                        .header("X-Repository-Role", "admin")
                        .file(file)
                        .param("requestedScopes", "SHELL_EXECUTE")
                        .param("requestedScopes", "FILES_WRITE"))
                .andDo(print())
                .andExpect(status().isOk()) // error 403
                .andReturn();

        JsonNode response = objectMapper.readTree(uploadResult.getResponse().getContentAsByteArray());
        System.out.println("uploadResponse=" + response.toPrettyString());

        assertThat(text(response.required("trustStatus"))).isEqualTo("QUARANTINED");
        assertThat(text(response.required("coordinate").required("groupId"))).isEqualTo(GROUP_ID);
        assertThat(text(response.required("coordinate").required("artifactId"))).isEqualTo(ARTIFACT_ID);
        assertThat(text(response.required("coordinate").required("version"))).isEqualTo(VERSION);
        assertThat(response.path("files")).isNotEmpty();
        assertThat(text(response.required("artifactChecksum").required("algorithm"))).isEqualTo("SHA-256");

        SqlArtifactMetadataStore store = new SqlArtifactMetadataStore(jdbcTemplate, repositoryProperties.sql(), objectMapper);
        ArtifactMetadataEntry entry = store.findArtifact(coordinate).orElseThrow();

        assertThat(entry.record().trustStatus().name()).isEqualTo("QUARANTINED");
        assertThat(entry.record().artifactUri()).startsWith("meshingress-repository://artifact/");
        assertThat(entry.record().files()).isNotEmpty();
        assertThat(Files.isRegularFile(entry.artifactPath())).isTrue();

        Path repositoryRoot = repositoryRoot();
        Path artifactPath = repositoryRoot
                .resolve("artifacts")
                .resolve(GROUP_ID.replace('.', '/'))
                .resolve(ARTIFACT_ID)
                .resolve(VERSION)
                .resolve(jar.getFileName().toString());
        Path quarantineRoot = repositoryRoot
                .resolve("quarantine")
                .resolve(GROUP_ID.replace('.', '/'))
                .resolve(ARTIFACT_ID)
                .resolve(VERSION);

        System.out.println("storedArtifactPath=" + entry.artifactPath().toAbsolutePath().normalize());
        System.out.println("expectedArtifactPath=" + artifactPath);
        System.out.println("quarantineRoot=" + quarantineRoot);
        System.out.println("quarantineEntries=" + countEntries(quarantineRoot));

        assertThat(entry.artifactPath()).isEqualTo(artifactPath);
        assertThat(Files.isDirectory(quarantineRoot)).isTrue();
        assertThat(countEntries(quarantineRoot)).isGreaterThan(0);

        MvcResult assessResult = mockMvc.perform(post("/artifact/" + GROUP_ID + "/" + ARTIFACT_ID + "/" + VERSION + "/assess")
                        .header("X-Repository-Role", "admin")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("REVIEW_PENDING"))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("cyclonedx-sbom")))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("embedded-jar-sandbox")))
                .andExpect(jsonPath("$.assessment.scanners", hasItem("bytecode-scope-scanner")))
                .andExpect(jsonPath("$.assessment.summary.sbom.format").value("CycloneDX"))
                .andExpect(jsonPath("$.assessment.summary.rawReportRetention.policy").value("retain-with-artifact"))
                .andExpect(jsonPath("$.assessment.summary.sandbox.strategy").value("static-quarantine-inspection"))
                .andReturn();

        JsonNode assessed = objectMapper.readTree(assessResult.getResponse().getContentAsByteArray());
        System.out.println("assessResponse=" + assessed.toPrettyString());
        assertThat(Files.exists(quarantineRoot)).isFalse();

        mockMvc.perform(get("/artifact/" + GROUP_ID + "/" + ARTIFACT_ID + "/" + VERSION + "/assessment")
                .header("X-Repository-Role", "admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanner").value("cyclonedx-sbom"))
                .andExpect(jsonPath("$[0].status").value("PASSED"))
                .andExpect(jsonPath("$[0].rawSummary.rawReport").value("cyclonedx-sbom.json"))
                .andExpect(jsonPath("$[0].rawReportPath").exists())
                .andExpect(jsonPath("$[1].scanner").value("embedded-jar-sandbox"))
                .andExpect(jsonPath("$[1].rawSummary.rawReport").value("embedded-jar-sandbox.json"))
                .andExpect(jsonPath("$[1].rawSummary.isolation.networkAccess").value(false))
                .andExpect(jsonPath("$[1].rawSummary.isolation.hostSecretsAccess").value(false))
                .andExpect(jsonPath("$[1].rawReportPath").exists())
                .andExpect(jsonPath("$[2].scanner").value("bytecode-scope-scanner"))
                .andExpect(jsonPath("$[2].status").value("REVIEW"));

        MvcResult approveResult = mockMvc.perform(post("/artifact/" + GROUP_ID + "/" + ARTIFACT_ID + "/" + VERSION + "/approve")
                        .header("X-Repository-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedScopes": ["ENVIRONMENT_WRITE", "FILES_DELETE", "FILES_WRITE", "SHELL_EXECUTE", "TOOLS_READ", "TOOLS_REGISTER"],
                                  "deniedScopes": [],
                                  "trustStatus": "APPROVED_LIMITED",
                                  "reviewer": "PowerShellCliArtifactRepositoryUploadInstanceTests"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.scopes.approvedScopes", hasItem("ENVIRONMENT_WRITE")))
                .andExpect(jsonPath("$.scopes.approvedScopes", hasItem("FILES_DELETE")))
                .andExpect(jsonPath("$.scopes.approvedScopes", hasItem("FILES_WRITE")))
                .andExpect(jsonPath("$.scopes.approvedScopes", hasItem("SHELL_EXECUTE")))
                .andExpect(jsonPath("$.scopes.approvedScopes", hasItem("TOOLS_READ")))
                .andExpect(jsonPath("$.scopes.approvedScopes", hasItem("TOOLS_REGISTER")))
                .andReturn();

        JsonNode approved = objectMapper.readTree(approveResult.getResponse().getContentAsByteArray());
        System.out.println("approveResponse=" + approved.toPrettyString());

        mockMvc.perform(post("/artifact/" + GROUP_ID + "/" + ARTIFACT_ID + "/" + VERSION + "/publish")
                .header("X-Repository-Role", "admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureKeyId").value("local-dev-hmac"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"))
                .andExpect(jsonPath("$.provenance.generatedBy").value("meshingress-repository"))
                .andExpect(jsonPath("$.signature", not(blankOrNullString())));

        mockMvc.perform(get("/artifact/" + GROUP_ID + "/" + ARTIFACT_ID + "/" + VERSION + "/publication")
                .header("X-Repository-Role", "admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustStatus").value("APPROVED_LIMITED"))
                .andExpect(jsonPath("$.signatureKeyId").value("local-dev-hmac"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("HmacSHA256"));

        ArtifactMetadataEntry reviewedEntry = store.findArtifact(coordinate).orElseThrow();
        assertThat(reviewedEntry.record().trustStatus().name()).isEqualTo("APPROVED_LIMITED");
        assertThat(store.findAssessment(coordinate)).hasSize(3);
        assertThat(store.findPublication(coordinate)).isPresent();
        assertThat(Files.isRegularFile(artifactPath.getParent().resolve("assessment.json"))).isTrue();
        assertThat(Files.isRegularFile(artifactPath.getParent().resolve("cyclonedx-sbom.json"))).isTrue();
        assertThat(Files.isRegularFile(artifactPath.getParent().resolve("embedded-jar-sandbox.json"))).isTrue();
        assertThat(Files.exists(repositoryRoot.resolve("assessments")
                .resolve(GROUP_ID.replace('.', '/'))
                .resolve(ARTIFACT_ID)
                .resolve(VERSION))).isFalse();

        System.out.println("=== PowerShell CLI repository upload/assessment smoke complete ===");
    }

    private Path packagedJar() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve("tools")
                    .resolve("lib")
                    .resolve(JAR_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path repositoryRoot() {
        return checkoutRoot().resolve("repository").toAbsolutePath().normalize();
    }

    private static Path checkoutRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            if (Files.isRegularFile(cursor.resolve("mvnw.cmd")) && Files.isRegularFile(cursor.resolve("pom.xml"))) {
                return cursor;
            }
        }
        throw new IllegalStateException("Unable to locate Meshingress checkout root from " + current);
    }

    private void resetCoordinate(ArtifactCoordinate coordinate) throws Exception {
        String coordinateKey = coordinate.display();
        MeshingressRepositoryProperties.Tables tables = repositoryProperties.sql().table();
        String schema = repositoryProperties.sql().schema();
        jdbcTemplate.update("delete from " + schema + "." + tables.lifecycleEvents() + " where coordinate_key = ?", coordinateKey);
        jdbcTemplate.update("delete from " + schema + "." + tables.publications() + " where coordinate_key = ?", coordinateKey);
        jdbcTemplate.update("delete from " + schema + "." + tables.assessments() + " where coordinate_key = ?", coordinateKey);
        jdbcTemplate.update("delete from " + schema + "." + tables.artifactFiles() + " where coordinate_key = ?", coordinateKey);
        jdbcTemplate.update("delete from " + schema + "." + tables.artifacts() + " where coordinate_key = ?", coordinateKey);

        Path groupPath = Path.of(GROUP_ID.replace('.', '/')).resolve(ARTIFACT_ID).resolve(VERSION);
        deleteRecursively(repositoryRoot().resolve("artifacts").resolve(groupPath));
        deleteRecursively(repositoryRoot().resolve("quarantine").resolve(groupPath));
        deleteRecursively(repositoryRoot().resolve("assessments").resolve(groupPath));
        deleteRecursively(repositoryRoot().resolve("reviews").resolve(groupPath));
        deleteRecursively(repositoryRoot().resolve("publications").resolve(groupPath));
    }

    private void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        assertThat(normalized).startsWith(repositoryRoot());
        try (var stream = Files.walk(normalized)) {
            for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    private long countEntries(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return 0L;
        }

        try (var stream = Files.list(directory)) {
            return stream.count();
        }
    }

    private String text(JsonNode node) {
        return node == null ? null : node.toString().replace("\"", "");
    }
}




