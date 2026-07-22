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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.voicebox.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HttpRouteOpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void artifactRoutesDocumentLifecycleAndRepositoryRoleHeaders() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/artifact/{groupId}/{artifactId}/{version}'].post.tags", hasItem("Artifact repository")))
                .andExpect(jsonPath("$.paths['/artifact/{groupId}/{artifactId}/{version}'].post.summary", is("Upload an artifact")))
                .andExpect(jsonPath("$.paths['/artifact/{groupId}/{artifactId}/{version}'].post.parameters[?(@.name == 'X-Repository-Role')].description", hasItem("Repository role or roles, separated by commas or whitespace. Required by the access policy; use uploader, reviewer, publisher, or admin as appropriate.")))
                .andExpect(jsonPath("$.paths['/artifact/{groupId}/{artifactId}/{version}/publish'].post.responses.403.description", is("The caller does not have the publisher or admin repository role.")));
    }

    @Test
    void storageRoutesDocumentRetrievalAndRangeBehavior() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/storage/{sessionId}/{requestId}/files/{relativePath}'].get.tags", hasItem("Workspace storage")))
                .andExpect(jsonPath("$.paths['/storage/{sessionId}/{requestId}/files/{relativePath}'].get.summary", is("Download a workspace file")))
                .andExpect(jsonPath("$.paths['/storage/{sessionId}/{requestId}/files/{relativePath}'].get.responses.416.description", is("A Range header was supplied; partial-content retrieval is not supported.")))
                .andExpect(jsonPath("$.paths['/storage/{sessionId}/{requestId}/files/{relativePath}'].head.summary", is("Inspect a workspace file")));
    }
}
