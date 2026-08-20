package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.identity.name=test-meshingress",
        "meshingress.identity.instance-id=test-node",
        "meshingress.dispatch.include-generated-at=false",
        "meshingress.tools.deny-list=helloworld.greeting.greet"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpPropertyPolicyTests {

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @Test
    void initializeUsesConfiguredIdentity() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-11-25"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverInfo.name", is("test-meshingress")))
                .andExpect(jsonPath("$.result.serverInfo.instanceId", is("test-node")));
    }

    @Test
    void toolDenyListRemovesFunctionFromListingAndCall() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 2,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", not(hasItem("helloworld.greeting.greet"))));

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 3,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "helloworld.greeting.greet",
                                    "arguments": {
                                      "name": "Meshingress"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.INVALID_PARAMS)));
    }

    @Test
    void generatedAtCanBeSuppressedForToolResults() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 4,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "architect.entries.list",
                                    "arguments": {
                                      "status": "resolved"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result._meta.generatedAt").doesNotExist())
                .andExpect(jsonPath("$.result._meta.tool.id", is("architect.entries")))
                .andExpect(jsonPath("$.result._meta.tool.name", is("architect.entries.list")));
    }
}
