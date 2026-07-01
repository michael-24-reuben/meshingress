package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactChecksum;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactProvenance;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.publication.HmacPublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.Ed25519PublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationSignature;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        "meshingress.repository.signing-secret=test-publication-secret"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpPublicationInstallTests {

    private static final String SAMPLE_JAR_NAME = "sample-module-0.0.1-SNAPSHOT-all.jar";
    private static final String GROUP_ID = "dev.mrk.tools";
    private static final String ARTIFACT_ID = "sample-module";
    private static final String VERSION = "0.0.1-SNAPSHOT";
    private static final String SIGNING_KEY_ID = "test-publication-key";
    private static final String SIGNING_SECRET = "test-publication-secret";
    private static final String ACTIVE_ED25519_KEY_ID = "test-ed25519-active";
    private static final String REVOKED_ED25519_KEY_ID = "test-ed25519-revoked";
    private static final KeyPair ACTIVE_ED25519_KEY_PAIR = ed25519KeyPair();
    private static final KeyPair REVOKED_ED25519_KEY_PAIR = ed25519KeyPair();

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanPersistentRuntimeState() throws Exception {
        deleteTree(tempDir.resolve("repository").resolve("runtime"));
        deleteTree(tempDir.resolve("runtime-cache"));
    }

    @DynamicPropertySource
    static void repositoryProperties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("meshingress.repository.runtime-cache-root", () -> tempDir.resolve("runtime-cache").toString());
        registry.add("meshingress.repository.verification-keys[0].key-id", () -> ACTIVE_ED25519_KEY_ID);
        registry.add("meshingress.repository.verification-keys[0].algorithm", () -> "Ed25519");
        registry.add("meshingress.repository.verification-keys[0].public-key", () -> Base64.getEncoder().encodeToString(ACTIVE_ED25519_KEY_PAIR.getPublic().getEncoded()));
        registry.add("meshingress.repository.verification-keys[0].status", () -> "ACTIVE");
        registry.add("meshingress.repository.verification-keys[1].key-id", () -> REVOKED_ED25519_KEY_ID);
        registry.add("meshingress.repository.verification-keys[1].algorithm", () -> "Ed25519");
        registry.add("meshingress.repository.verification-keys[1].public-key", () -> Base64.getEncoder().encodeToString(REVOKED_ED25519_KEY_PAIR.getPublic().getEncoded()));
        registry.add("meshingress.repository.verification-keys[1].status", () -> "REVOKED");
    }

    @Test
    void adminCanInstallPublicationSignedByActiveEd25519Key() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = ed25519SignedPublication(
                repositoryJar,
                ACTIVE_ED25519_KEY_ID,
                ACTIVE_ED25519_KEY_PAIR
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(521, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.installed", is(true)));
    }

    @Test
    void installRejectsPublicationSignedByRevokedEd25519Key() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = ed25519SignedPublication(
                repositoryJar,
                REVOKED_ED25519_KEY_ID,
                REVOKED_ED25519_KEY_PAIR
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(522, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication signing key is revoked.")));
    }

    @Test
    void adminCanInstallApprovedPublicationRecord() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(500, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.installed", is(true)))
                .andExpect(jsonPath("$.result.sourceKind", is("PUBLICATION_RECORD")))
                .andExpect(jsonPath("$.result.registeredFunctions[*]", hasItem("helloworld.text")));

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 501,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "helloworld.text",
                                    "arguments": {}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(false)));
    }

    @Test
    void adminCanDeleteInstalledPublicationAndRemoveRuntimeCache() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(530, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.installed", is(true)));

        Path cachedJar = tempDir.resolve("runtime-cache")
                .resolve(GROUP_ID.replace('.', '/'))
                .resolve(ARTIFACT_ID)
                .resolve(VERSION)
                .resolve(SAMPLE_JAR_NAME);
        assertThat(cachedJar).isRegularFile();

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteRequest(531, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deleted", is(true)))
                .andExpect(jsonPath("$.result.runtimeDeactivated", is(1)))
                .andExpect(jsonPath("$.result.runtimeCacheRemoved", is(1)))
                .andExpect(jsonPath("$.result.registrations[0].status", is("deleted")))
                .andExpect(jsonPath("$.result.registrations[0].runtimeDeactivated", is(true)))
                .andExpect(jsonPath("$.result.registrations[0].runtimeCacheRemoved", is(true)));

        assertThat(cachedJar).doesNotExist();

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 532,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "helloworld.text",
                                    "arguments": {}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.INVALID_PARAMS)))
                .andExpect(jsonPath("$.error.message", is("Tool function is not available.")));
    }

    @Test
    void installRejectsInvalidPublicationSignature() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );
        ArtifactPublicationRecord tampered = new ArtifactPublicationRecord(
                publication.coordinate(),
                publication.type(),
                publication.trustStatus(),
                publication.artifactUri(),
                publication.artifactChecksum(),
                new ArtifactScopeDeclaration(List.of(), List.of(), List.of("FILES_READ"), List.of()),
                publication.scanSummary(),
                publication.provenance(),
                publication.revoked(),
                publication.publishedAt(),
                publication.signatureKeyId(),
                publication.signatureAlgorithm(),
                publication.signature()
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(510, tampered, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication record signature is invalid.")));
    }

    @Test
    void installRejectsTamperedPublicationProvenance() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );
        ArtifactPublicationRecord tampered = new ArtifactPublicationRecord(
                publication.coordinate(),
                publication.type(),
                publication.trustStatus(),
                publication.artifactUri(),
                publication.artifactChecksum(),
                publication.scopePolicy(),
                publication.scanSummary(),
                new ArtifactProvenance("https://example.invalid/repo.git", "tampered", "tampered-builder", ""),
                publication.revoked(),
                publication.publishedAt(),
                publication.signatureKeyId(),
                publication.signatureAlgorithm(),
                publication.signature()
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(511, tampered, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication record signature is invalid.")));
    }

    @Test
    void installRejectsUnknownPublicationSigningKeyId() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );
        ArtifactPublicationRecord unknownKey = new ArtifactPublicationRecord(
                publication.coordinate(),
                publication.type(),
                publication.trustStatus(),
                publication.artifactUri(),
                publication.artifactChecksum(),
                publication.scopePolicy(),
                publication.scanSummary(),
                publication.provenance(),
                publication.revoked(),
                publication.publishedAt(),
                "unknown-key",
                publication.signatureAlgorithm(),
                publication.signature()
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(517, unknownKey, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Unsupported publication signing key id.")));
    }

    @Test
    void installRejectsUnsignedPublicationRecord() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord unsigned = unsignedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(512, unsigned, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication record is unsigned.")));
    }

    @Test
    void installRejectsChecksumMismatch() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false,
                ArtifactChecksum.sha256("deadbeef")
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(513, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("repository artifact checksum mismatch")));
    }

    @Test
    void installRejectsRevokedPublicationRecord() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                true
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(514, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication record is revoked.")));
    }

    @Test
    void installRejectsNonInstallableTrustStatus() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.REJECTED,
                false
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(515, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication record is not in an installable trust state.")));
    }

    @Test
    void installRejectsUnapprovedFunctionScope() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of(),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(516, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Tool function helloworld.text scope not approved in publication: USER_WRITE")));
    }

    @Test
    void installRejectsLocallyDisabledApprovedScope() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        Path repositoryJar = installRepositoryArtifact(sampleJar);

        ArtifactPublicationRecord publication = signedPublication(
                repositoryJar,
                List.of("USER_WRITE", "SHELL_EXECUTE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(installRequest(520, publication, "helloworld.text")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.message", is("Publication record approves disabled scope: SHELL_EXECUTE")));
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

    private String deleteRequest(int id, String toolId) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "roles/tools/delete",
                  "params": {
                    "name": "%s",
                    "mode": "disable"
                  }
                }
                """.formatted(id, toolId);
    }

    private ArtifactPublicationRecord signedPublication(
            Path repositoryJar,
            List<String> approvedScopes,
            ArtifactTrustStatus trustStatus,
            boolean revoked
    ) throws Exception {
        return signedPublication(repositoryJar, approvedScopes, trustStatus, revoked, ArtifactChecksum.sha256(sha256(repositoryJar)));
    }

    private ArtifactPublicationRecord signedPublication(
            Path repositoryJar,
            List<String> approvedScopes,
            ArtifactTrustStatus trustStatus,
            boolean revoked,
            ArtifactChecksum checksum
    ) throws Exception {
        ArtifactCoordinate coordinate = new ArtifactCoordinate(GROUP_ID, ARTIFACT_ID, VERSION, null, "jar");
        ArtifactPublicationRecord unsigned = new ArtifactPublicationRecord(
                coordinate,
                MeshingressArtifactType.GENERATED_TOOL_MODULE,
                trustStatus,
                "meshingress-repository://artifact/%s/%s/%s/%s".formatted(GROUP_ID, ARTIFACT_ID, VERSION, SAMPLE_JAR_NAME),
                checksum,
                new ArtifactScopeDeclaration(approvedScopes, approvedScopes, approvedScopes, List.of()),
                new ArtifactAssessmentSummary("clean", List.of("cyclonedx-sbom", "bytecode-scope-scanner"), 0, Map.of("test", true)),
                null,
                revoked,
                OffsetDateTime.parse("2026-05-29T00:00:00-04:00"),
                SIGNING_KEY_ID,
                "HmacSHA256",
                ""
        );
        PublicationSignature signature = new HmacPublicationRecordSigner(SIGNING_KEY_ID, SIGNING_SECRET).sign(objectMapper.writeValueAsString(unsigned));
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

    private ArtifactPublicationRecord unsignedPublication(
            Path repositoryJar,
            List<String> approvedScopes,
            ArtifactTrustStatus trustStatus,
            boolean revoked
    ) throws Exception {
        return unsignedPublication(repositoryJar, approvedScopes, trustStatus, revoked, ArtifactChecksum.sha256(sha256(repositoryJar)));
    }

    private ArtifactPublicationRecord ed25519SignedPublication(Path repositoryJar, String keyId, KeyPair keyPair) throws Exception {
        ArtifactPublicationRecord unsigned = unsignedPublication(
                repositoryJar,
                List.of("USER_WRITE"),
                ArtifactTrustStatus.APPROVED_LIMITED,
                false
        );
        unsigned = new ArtifactPublicationRecord(
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
                keyId,
                "Ed25519",
                ""
        );
        PublicationSignature signature = new Ed25519PublicationRecordSigner(keyId, keyPair.getPrivate())
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

    private static KeyPair ed25519KeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Ed25519 test key pair", exception);
        }
    }

    private ArtifactPublicationRecord unsignedPublication(
            Path repositoryJar,
            List<String> approvedScopes,
            ArtifactTrustStatus trustStatus,
            boolean revoked,
            ArtifactChecksum checksum
    ) {
        ArtifactCoordinate coordinate = new ArtifactCoordinate(GROUP_ID, ARTIFACT_ID, VERSION, null, "jar");
        return new ArtifactPublicationRecord(
                coordinate,
                MeshingressArtifactType.GENERATED_TOOL_MODULE,
                trustStatus,
                "meshingress-repository://artifact/%s/%s/%s/%s".formatted(GROUP_ID, ARTIFACT_ID, VERSION, SAMPLE_JAR_NAME),
                checksum,
                new ArtifactScopeDeclaration(approvedScopes, approvedScopes, approvedScopes, List.of()),
                new ArtifactAssessmentSummary("clean", List.of("cyclonedx-sbom", "bytecode-scope-scanner"), 0, Map.of("test", true)),
                null,
                revoked,
                OffsetDateTime.parse("2026-05-29T00:00:00-04:00"),
                SIGNING_KEY_ID,
                "HmacSHA256",
                ""
        );
    }

    private Path installRepositoryArtifact(Path sourceJar) throws Exception {
        Path repositoryJar = tempDir.resolve("repository")
                .resolve("artifacts")
                .resolve(GROUP_ID.replace('.', '/'))
                .resolve(ARTIFACT_ID)
                .resolve(VERSION)
                .resolve(SAMPLE_JAR_NAME);
        Files.createDirectories(repositoryJar.getParent());
        Files.copy(sourceJar, repositoryJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return repositoryJar;
    }

    private void deleteTree(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (var files = Files.walk(path)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private Path sampleJar() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path cursor = current;
        for (int i = 0; i < 4 && cursor != null; i++) {
            Path candidate = cursor.resolve("temp").resolve(SAMPLE_JAR_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        return current.resolve("temp").resolve(SAMPLE_JAR_NAME);
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
