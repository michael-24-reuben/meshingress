package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "meshingress.architect.root=../../architect")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpYoutubeToolMvcTests {
    @Autowired private MockMvc mockMvc;

    @Test
    void toolsListExposesImplementedYoutubeCatalogAndPublicDataFunctions() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":802,"method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItems(
                        "youtube.catalog.capabilities",
                        "youtube.catalog.providers",
                        "youtube.data.search",
                        "youtube.data.videos-get",
                        "youtube.data.channels-get",
                        "youtube.data.playlists-get"
                )))
                .andExpect(jsonPath("$.result.tools[*].name", not(hasItems(
                        "youtube.creator.video-upload",
                        "youtube.transcript.get"
                ))))
                .andExpect(jsonPath("$.result.tools[?(@.name == 'youtube.catalog.capabilities')].outputSchema.properties.data.properties.records.items.properties.function.type").value("string"))
                .andExpect(jsonPath("$.result.tools[?(@.name == 'youtube.catalog.providers')].outputSchema.properties.data.properties.records.items.properties.id.type").value("string"))
                .andExpect(jsonPath("$.result.tools[?(@.name == 'youtube.data.search')].outputSchema.properties.data.properties.resource.type").value("string"))
                .andExpect(jsonPath("$.result.tools[?(@.name == 'youtube.data.videos-get')].outputSchema.properties.data.properties.response.type").value("object"));
    }

    @Test
    void publicDataCallsFailClosedWhenTheProductionApiKeyIsBlank() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":803,"method":"tools/call","params":{"name":"youtube.data.search","arguments":{"query":"meshingress"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(jsonPath("$.result._meta.errorCode").value("YOUTUBE_API_KEY_NOT_CONFIGURED"));
    }
}
