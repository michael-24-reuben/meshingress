package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = "meshingress.architect.root=../../architect")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpControllerOutputTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void initializeReturnsToolCapabilities() throws Exception {
        printResponse("initializeReturnsToolCapabilities",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-11-25",
                                    "capabilities": {},
                                    "clientInfo": {
                                      "name": "test-client",
                                      "version": "0.1.0"
                                    }
                                  }
                                }
                                """));
    }

    @Test
    void toolsListIncludesStaticArchitectTool() throws Exception {
        printResponse("toolsListIncludesStaticArchitectTool",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 2,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """));
    }

    @Test
    void toolsListIncludesAttachedHelloWorldModule() throws Exception {
        printResponse("toolsListIncludesAttachedHelloWorldModule",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 20,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """));
    }

    @Test
    void toolsCallInvokesAttachedHelloWorldModule() throws Exception {
        printResponse("toolsCallInvokesAttachedHelloWorldModule",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 21,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "helloworld.greeting.greet",
                                    "arguments": {
                                      "name": "Meshingress"
                                    }
                                  }
                                }
                                """));
    }

    @Test
    void toolsListIncludesAttachedInstaFetchModule() throws Exception {
        printResponse("toolsListIncludesAttachedInstaFetchModule",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 20,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """));
    }

    @Test
    void toolsCallInvokesAttachedInstaFetchModule() throws Exception {
        printResponse("toolsCallInvokesAttachedInstaFetchModule",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 30,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "instagram.fetch",
                                    "arguments": {
                                      "url": "https://www.instagram.com/reel/DWCd2FtkfTj/"
                                    }
                                  }
                                }
                                """));
    }

    @Test
    void toolsCallReturnsArchitectEntries() throws Exception {
        printResponse("toolsCallReturnsArchitectEntries",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 3,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "architect.entries.list",
                                    "arguments": {
                                      "status": "resolved"
                                    }
                                  }
                                }
                                """));
    }

    @Test
    void invalidJsonRpcEnvelopeReturnsProtocolError() throws Exception {
        printResponse("invalidJsonRpcEnvelopeReturnsProtocolError",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "1.0",
                                  "id": 4,
                                  "method": "ping"
                                }
                                """));
    }

    @Test
    void roleMethodsRequireAdminAuthorization() throws Exception {
        printResponse("roleMethodsRequireAdminAuthorization",
                post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 5,
                                  "method": "roles/tools/list",
                                  "params": {
                                    "includeDisabled": true,
                                    "includePrivate": true
                                  }
                                }
                                """));
    }

    @Test
    void roleAdminCanCheckAliasUpdateAndDisableTool() throws Exception {
        printResponse("roles/tools/check",
                post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolRequest(6, "roles/tools/check")));

        printResponse("roles/tools/alias",
                post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolRequest(7, "roles/tools/alias")));

        printResponse("roles/tools/update",
                post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 8,
                                  "method": "roles/tools/update",
                                  "params": {
                                    "name": "architect.alias",
                                    "patch": {
                                      "description": "List architect entries through a dynamic alias.",
                                      "enabled": true
                                    }
                                  }
                                }
                                """));

        printResponse("roles/tools/delete",
                post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 9,
                                  "method": "roles/tools/delete",
                                  "params": {
                                    "name": "architect.alias",
                                    "mode": "disable"
                                  }
                                }
                                """));
    }

    @Test
    void reservedMcpMethodsHaveMvpResponses() throws Exception {
        printResponse("GET /mcp", get("/mcp"));
        printResponse("DELETE /mcp", delete("/mcp"));
    }

    private void printResponse(String label, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();

        System.out.println();
        System.out.println("=== " + label + " ===");
        System.out.println("HTTP " + result.getResponse().getStatus());
        System.out.println(result.getResponse().getContentAsString());
        System.out.println("====================");
    }

    private String toolRequest(int id, String method) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "%s",
                  "params": {
                    "tool": {
                      "name": "architect.alias",
                      "title": "List Architect Entries Alias",
                      "description": "List architect entries through a dynamic alias.",
                      "enabled": true,
                      "visibility": "public",
                      "functions": [{
                        "name": "architect.alias.copy",
                        "title": "List Architect Entries Alias",
                        "description": "List architect entries through a dynamic alias.",
                        "enabled": true,
                        "visibility": "public",
                        "handlerKey": "architect.entries.list",
                        "inputSchema": {
                          "type": "object",
                          "properties": {
                            "status": {
                              "type": "string"
                            }
                          },
                          "additionalProperties": false
                        }
                      }]
                    }
                  }
                }
                """.formatted(id, method);
    }
}
