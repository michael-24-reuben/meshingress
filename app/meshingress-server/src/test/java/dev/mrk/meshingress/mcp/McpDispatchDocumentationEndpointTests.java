package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.voicebox.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpDispatchDocumentationEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dispatchDocsExposeAnnotatedMethods() throws Exception {
        mockMvc.perform(get("/v3/dispatch-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x-dispatch-map.methods[*].method", hasItem("storage/publication-status")))
                .andExpect(jsonPath("$.x-dispatch-map.methods[*].method", hasItem("roles/tools/reload")))
                .andExpect(jsonPath("$.x-dispatch-map.methods[*].method", hasItem("roles/tools/contributions/list")))
                .andExpect(jsonPath("$.x-dispatch-map.methods[*].method", hasItem("roles/tools/contributions/update")))
                .andExpect(jsonPath("$.components.schemas.McpStoragePublicationStatusRequest.properties.method.example", is("storage/publication-status")));
    }

    @Test
    void dispatchYamlEndpointReturnsYaml() throws Exception {
        mockMvc.perform(get("/v3/dispatch-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/yaml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("storage/publication-status")));
    }

    @Test
    void openApiAliasesRedirectToSpringdocEndpoints() throws Exception {
        mockMvc.perform(get("/v3/openapi-docs.yaml"))
                .andExpect(status().isTemporaryRedirect())
                .andExpect(header().string("Location", "/v3/api-docs.yaml"));
    }
}
