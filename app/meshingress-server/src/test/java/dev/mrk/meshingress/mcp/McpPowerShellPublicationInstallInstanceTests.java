package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactChecksum;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.publication.HmacPublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.tools.registry.enabled=true",
        "meshingress.tools.registry.fail-on-duplicate-tool-id=true",
        "meshingress.tools.registry.fail-on-invalid-tool-id=true",
        "meshingress.tools.registry.include-disabled=false",
        "meshingress.tools.registry.scan-on-startup=false",
        "meshingress.tools.registry.expose-private-tools=false",
        "meshingress.repository.signing-key-id=test-publication-key",
        "meshingress.repository.signing-secret=test-publication-secret",
        "meshingress.scopes.allow-shell-execute=true",
        "meshingress.scopes.allow-files-delete=true"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpPowerShellPublicationInstallInstanceTests {

    private static final String JAR_NAME = "powershell-cli-0.0.1-SNAPSHOT.jar";
    private static final String GROUP_ID = "dev.mrk.toolspace";
    private static final String ARTIFACT_ID = "powershell-cli";
    private static final String VERSION = "0.0.1-SNAPSHOT";
    private static final String SIGNING_KEY_ID = "test-publication-key";
    private static final String SIGNING_SECRET = "test-publication-secret";
    private static final List<String> APPROVED_SCOPES = List.of("FILES_DELETE", "FILES_WRITE", "SHELL_EXECUTE");

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("meshingress.repository.runtime-cache-root", () -> tempDir.resolve("runtime-cache").toString());
    }

    @Test
    void adminCanInstallApprovedPowerShellPublicationRecord() throws Exception {
        Path packagedJar = packagedPowerShellJar();
        assumeTrue(Files.isRegularFile(packagedJar), "PowerShell CLI jar is missing: " + packagedJar);
        Path repositoryJar = installRepositoryArtifact(packagedJar);
        ArtifactPublicationRecord publication = signedPublication(repositoryJar);

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(700, publication, "cli.powershell")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.installed", is(true)))
                .andExpect(jsonPath("$.result.sourceKind", is("PUBLICATION_RECORD")))
                .andExpect(jsonPath("$.result.registeredFunctions[*]", hasItem("powershell.cli.execute")));
    }

    private String installRequest(int id, ArtifactPublicationRecord publication, String toolId) throws Exception {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "roles/tools/installPublication",
                  "params": {
                    "toolId": "%s",
                    "publication": %s
                  }
                }
                """.formatted(id, toolId, objectMapper.writeValueAsString(publication));
    }

    private ArtifactPublicationRecord signedPublication(Path repositoryJar) throws Exception {
        ArtifactCoordinate coordinate = new ArtifactCoordinate(GROUP_ID, ARTIFACT_ID, VERSION, null, "jar");
        ArtifactPublicationRecord unsigned = new ArtifactPublicationRecord(
                coordinate,
                MeshingressArtifactType.TOOL_MODULE,
                ArtifactTrustStatus.APPROVED_LIMITED,
                "meshingress-repository://artifact/%s/%s/%s/%s".formatted(GROUP_ID, ARTIFACT_ID, VERSION, JAR_NAME),
                ArtifactChecksum.sha256(sha256(repositoryJar)),
                new ArtifactScopeDeclaration(
                        List.of("SHELL_EXECUTE", "FILES_WRITE"),
                        APPROVED_SCOPES,
                        APPROVED_SCOPES,
                        List.of()
                ),
                new ArtifactAssessmentSummary(
                        "findings",
                        List.of("cyclonedx-sbom", "bytecode-scope-scanner"),
                        10,
                        Map.of("reviewed", true)
                ),
                null,
                false,
                OffsetDateTime.parse("2026-06-14T00:00:00-04:00"),
                SIGNING_KEY_ID,
                "HmacSHA256",
                ""
        );
        PublicationSignature signature = new HmacPublicationRecordSigner(SIGNING_KEY_ID, SIGNING_SECRET)
                .sign(objectMapper.writeValueAsString(unsigned));
        return new ArtifactPublicationRecord(
                unsigned.coordinate(),
                unsigned.type(),
                unsigned.trustStatus(),
                unsigned.artifactUri(),
                unsigned.artifactChecksum(),
                unsigned.scopePolicy(),
                unsigned.scanSummary(),
                unsigned.provenance(),
                unsigned.revoked(),
                unsigned.publishedAt(),
                signature.keyId(),
                signature.algorithm(),
                signature.value()
        );
    }

    private Path installRepositoryArtifact(Path sourceJar) throws Exception {
        Path repositoryJar = tempDir.resolve("repository")
                .resolve("artifacts")
                .resolve(GROUP_ID.replace('.', '/'))
                .resolve(ARTIFACT_ID)
                .resolve(VERSION)
                .resolve(JAR_NAME);
        Files.createDirectories(repositoryJar.getParent());
        Files.copy(sourceJar, repositoryJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return repositoryJar;
    }

    private Path packagedPowerShellJar() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve("temp").resolve("packages")
                    .resolve("powershell-cli")
                    .resolve("target")
                    .resolve(JAR_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return current.resolve("temp").resolve("packages").resolve("powershell-cli").resolve("target").resolve(JAR_NAME);
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
