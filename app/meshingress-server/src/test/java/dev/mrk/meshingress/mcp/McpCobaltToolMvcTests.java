package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.cobalt.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpCobaltToolMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void toolsListIncludesCobaltFunctions() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 601,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("cobalt.info")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("cobalt.process")));
    }

    @Test
    void toolsCallReportsCobaltUnavailableWhenBackendIsNotRunning() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 602,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "cobalt.info",
                                    "arguments": {}
                                  }
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(true)))
                .andExpect(jsonPath("$.result.structuredContent.data.ok", is(false)))
                .andExpect(jsonPath("$.result.structuredContent.data.details.upstreamPath", is("toolspace/cobalt/upstream/cobalt")))
                .andExpect(jsonPath("$.result._meta.errorCode", is("COBALT_UNAVAILABLE")))
                .andExpect(jsonPath("$.result._meta.errorMessage", containsString("Start a local Cobalt API instance")))
                .andExpect(jsonPath("$.result._meta.tool.id", is("cobalt")))
                .andExpect(jsonPath("$.result._meta.tool.name", is("cobalt.info")))
                .andExpect(jsonPath("$.result._meta.tool.title", is("Cobalt")))
                .andExpect(jsonPath("$.result._meta.tool.function", is("info")))
                .andExpect(jsonPath("$.result._meta.tool.functionTitle", is("Cobalt Info")));
    }
}
