package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.tools.registration.enabled=true",
        "meshingress.tools.registration.allow-experimental=true",
        "meshingress.tools.registration.allow-staging=true",
        "meshingress.tools.registration.allow-bundle=true",
        "meshingress.tools.registration.local-jar-root=../../temp",
        "meshingress.tools.registration.bundle-pom-path=../meshingress-tool-bundle/pom.xml",
        "meshingress.tools.registration.allow-native-http=true"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpToolRegistrationPhaseApiSampleTests {

    private static final String SAMPLE_JAR_NAME = "sample-module-0.0.1-SNAPSHOT-all.jar";
    private static final String SAMPLE_GROUP_ID = "dev.mrk.toolspace";
    private static final String SAMPLE_ARTIFACT_ID = "sample-module";
    private static final String SAMPLE_VERSION = "0.0.1-SNAPSHOT";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerExperimentalLocalJarToolThroughMcpApi() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);

        printMcpResponse("experimental/local-jar register",
                mcpRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 100,
                          "method": "roles/tools/register",
                          "params": {
                            "phase": "experimental",
                            "toolId": "helloworld.text",
                            "replace": true,
                            "localJar": {
                              "path": "%s",
                              "checksumSha256": "%s"
                            }
                          }
                        }
                        """.formatted(SAMPLE_JAR_NAME, sha256(sampleJar))));
        printMcpResponse("experimental/local-jar tools/list", toolsListRequest(101));
        printMcpResponse("experimental/local-jar tools/call", toolCallRequest(102, "helloworld.text", "{}"));
    }

    @Test
    void rejectExperimentalLocalJarToolWhenChecksumDoesNotMatch() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);

        mockMvc.perform(mcpRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 110,
                          "method": "roles/tools/register",
                          "params": {
                            "phase": "experimental",
                            "toolId": "checksum.mismatch",
                            "replace": true,
                            "localJar": {
                              "path": "%s",
                              "checksumSha256": "0000000000000000000000000000000000000000000000000000000000000000"
                            }
                          }
                        }
                        """.formatted(SAMPLE_JAR_NAME)))
                .andExpect(jsonPath("$.jsonrpc", is("2.0")))
                .andExpect(jsonPath("$.id", is(110)))
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.INVALID_PARAMS)))
                .andExpect(jsonPath("$.error.data.errorCode", is("TOOL_REGISTRATION_CHECKSUM_MISMATCH")));
    }

    @Test
    void registerStagingMavenCoordinatesToolThroughMcpApi() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);
        installSampleJarInLocalMavenRepository(sampleJar);

        printMcpResponse("staging/maven register",
                mcpRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 200,
                          "method": "roles/tools/register",
                          "params": {
                            "phase": "staging",
                            "toolId": "helloworld.text",
                            "replace": true,
                            "maven": {
                              "groupId": "dev.mrk.toolspace",
                              "artifactId": "sample-module",
                              "version": "0.0.1-SNAPSHOT"
                            }
                          }
                        }
                        """));
        printMcpResponse("staging/maven tools/list", toolsListRequest(201));
        printMcpResponse("staging/maven tools/call", toolCallRequest(202, "helloworld.text", "{}"));
    }

    @Test
    void registerBundledToolThroughMcpApi() throws Exception {
        Path sampleJar = sampleJar();
        assumeTrue(Files.isRegularFile(sampleJar), "Smoke fixture is missing: " + sampleJar);

        printMcpResponse("bundle/maven-to-module register",
                mcpRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 300,
                          "method": "roles/tools/register",
                          "params": {
                            "phase": "bundle",
                            "toolId": "helloworld.text",
                            "bundle": {
                              "bundleId": "meshingress-tool-bundle"
                            },
                            "localJar": {
                              "path": "%s",
                              "checksumSha256": "%s"
                            },
                            "maven": {
                              "groupId": "dev.mrk.toolspace",
                              "artifactId": "sample-module",
                              "version": "0.0.1-SNAPSHOT"
                            }
                          }
                        }
                        """.formatted(SAMPLE_JAR_NAME, sha256(sampleJar))));
        printMcpResponse("bundle/maven-to-module tools/list", toolsListRequest(301));
        printMcpResponse("bundle/maven-to-module tools/call", toolCallRequest(302, "helloworld.text", "{}"));
    }

    @Test
    void registerNativeServerToolThroughMcpApi() throws Exception {
        printMcpResponse("native/server-core register",
                mcpRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 400,
                          "method": "roles/tools/register",
                          "params": {
                            "phase": "native",
                            "toolId": "meshingress.runtime.info",
                            "nativeTool": {
                              "namespace": "meshingress.runtime"
                            }
                          }
                        }
                        """));
        printMcpResponse("native/server-core tools/list", toolsListRequest(401));
        printMcpResponse("native/server-core tools/call", toolCallRequest(402, "meshingress.runtime.info", "{}"));
    }

    private MockHttpServletRequestBuilder mcpRequest(String json) {
        return post("/mcp")
                .header("Authorization", "Bearer dev-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private MockHttpServletRequestBuilder toolsListRequest(int id) {
        return post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "jsonrpc": "2.0",
                          "id": %d,
                          "method": "tools/list",
                          "params": {}
                        }
                        """.formatted(id));
    }

    private MockHttpServletRequestBuilder toolCallRequest(int id, String name, String arguments) {
        return post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "jsonrpc": "2.0",
                          "id": %d,
                          "method": "tools/call",
                          "params": {
                            "name": "%s",
                            "arguments": %s
                          }
                        }
                        """.formatted(id, name, arguments));
    }

    private void printMcpResponse(String label, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();

        System.out.println();
        System.out.println("=== " + label + " ===");
        System.out.println("HTTP " + result.getResponse().getStatus());
        System.out.println(result.getResponse().getContentAsString());
        System.out.println("====================");
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

    /**
     * Test fixture helper only.
     * <p>
     * This writes a sample artifact directly into the local Maven repository so
     * staging registration MCP flows can run in this test class. It does not
     * emulate production/repository artifact installation behavior.
     * <p>
     * The real jar installation/repository artifact logic is planned under the
     * architect draft `2026-05-27-meshingress-repository-artifact-implementation`.
     */
    private void installSampleJarInLocalMavenRepository(Path sampleJar) throws IOException {
        Path artifactDirectory = Path.of(System.getProperty("user.home"), ".m2", "repository")
                .resolve(SAMPLE_GROUP_ID.replace('.', '/'))
                .resolve(SAMPLE_ARTIFACT_ID)
                .resolve(SAMPLE_VERSION);
        Path artifactJar = artifactDirectory.resolve(SAMPLE_ARTIFACT_ID + "-" + SAMPLE_VERSION + ".jar");
        Files.createDirectories(artifactDirectory);
        if (!Files.isRegularFile(artifactJar)) {
            Files.copy(sampleJar, artifactJar, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(
                artifactDirectory.resolve(SAMPLE_ARTIFACT_ID + "-" + SAMPLE_VERSION + ".pom"),
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>dev.mrk.toolspace</groupId>
                          <artifactId>sample-module</artifactId>
                          <version>0.0.1-SNAPSHOT</version>
                        </project>
                        """
        );
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    @TestConfiguration
    static class NativeSampleToolConfiguration {

        @Bean
        McpToolHandler meshingressRuntimeInfoTool(tools.jackson.databind.ObjectMapper objectMapper) {
            ObjectNode inputSchema = objectMapper.createObjectNode();
            inputSchema.put("type", "object");
            inputSchema.set("properties", objectMapper.createObjectNode());
            inputSchema.put("additionalProperties", false);

            McpFunctionDescriptor function = new McpFunctionDescriptor(
                    "meshingress.runtime.info",
                    "Meshingress Runtime Info",
                    "Return runtime information for API registration samples.",
                    1,
                    true,
                    ToolVisibility.PUBLIC,
                    "meshingress.runtime.info",
                    inputSchema,
                    null,
                    null,
                    false
            );
            McpToolDescriptor descriptor = new McpToolDescriptor(
                    "meshingress.runtime",
                    "Meshingress Runtime",
                    "Server-native runtime sample tool.",
                    1,
                    true,
                    ToolVisibility.PUBLIC,
                    List.of(function),
                    null,
                    false
            );

            return new McpToolHandler() {
                @Override
                public McpToolDescriptor descriptor() {
                    return descriptor;
                }

                @Override
                public DispatchExecutionResult call(ObjectNode arguments, McpCallContext context) {
                    ObjectNode structured = objectMapper.createObjectNode();
                    structured.put("sourceKind", "SERVER_NATIVE");
                    structured.put("function", "meshingress.runtime.info");
                    structured.put("requestId", context.requestId());
                    return DispatchExecutionResult.builder()
                            .structuredContent(structured)
                            .build();
                }
            };
        }
    }
}
