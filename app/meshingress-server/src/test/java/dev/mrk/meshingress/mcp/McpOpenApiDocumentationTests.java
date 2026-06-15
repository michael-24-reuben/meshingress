package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.voicebox.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpOpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mcpPostDocumentsJsonRpcRequestSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/mcp'].post.requestBody.content['application/json'].schema.oneOf[*].$ref", hasItems(
                        "#/components/schemas/McpToolsCallRequest",
                        "#/components/schemas/McpRolesToolsRegisterRequest",
                        "#/components/schemas/McpJsonRpcBatchRequest"
                )))
                .andExpect(jsonPath("$.components.schemas.ToolRegistrationParams.description", is("Phase-aware parameters for roles/tools/register.")))
                .andExpect(jsonPath("$.components.schemas.ToolRegistrationParams.properties.localJar.description", is("Local JAR source details.")))
                .andExpect(jsonPath("$.components.schemas.ToolRegistrationLocalJarParams.properties.checksumSha256.description", is("Expected SHA-256 checksum for the JAR.")))
                .andExpect(jsonPath("$.components.schemas.McpToolsCallParams.properties.arguments.description", is("Tool-specific arguments object. Its schema is supplied by tools/list for each tool.")));
    }
}
