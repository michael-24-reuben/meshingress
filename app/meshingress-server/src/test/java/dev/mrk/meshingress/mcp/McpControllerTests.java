package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "meshingress.architect.root=../../architect")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mcpCorsPreflightAllowsBrowserToolClientOrigin() throws Exception {
        mockMvc.perform(options("/mcp")
                        .header("Origin", "http://127.0.0.1:4738")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-request-id,authorization,x-mcp-session-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:4738"))
                .andExpect(header().string("Access-Control-Allow-Methods", "POST,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "content-type, x-request-id, authorization, x-mcp-session-id"));
    }

    @Test
    void initializeReturnsToolCapabilities() throws Exception {
        mockMvc.perform(post("/mcp")
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc", is("2.0")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.result.capabilities.tools.listChanged", is(true)))
                .andExpect(jsonPath("$.result.serverInfo.name", is("meshingress")));
    }

    @Test
    void toolsListIncludesStaticArchitectTool() throws Exception {
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
                .andExpect(jsonPath("$.result.tools[0].name", is("architect.entries.list")))
                .andExpect(jsonPath("$.result.tools[0].annotations.readOnlyHint", is(true)));
    }

    @Test
    void toolsListIncludesAttachedHelloWorldModule() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 20,
                                  "method": "tools/list",
                                  "params": {}
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("helloworld.greet")));
    }

    @Test
    void toolsCallInvokesAttachedHelloWorldModule() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 21,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "helloworld.greet",
                                    "arguments": {
                                      "name": "Meshingress"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(false)))
                .andExpect(jsonPath("$.result.structuredContent.message", is("Hello, Meshingress!")));
    }

    @Test
    void toolsCallReturnsArchitectEntries() throws Exception {
        mockMvc.perform(post("/mcp")
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(false)))
                .andExpect(jsonPath("$.result.structuredContent.entries.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void invalidJsonRpcEnvelopeReturnsProtocolError() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "1.0",
                                  "id": 4,
                                  "method": "ping"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.INVALID_REQUEST)));
    }

    @Test
    void roleMethodsRequireAdminAuthorization() throws Exception {
        mockMvc.perform(post("/mcp")
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
                                """))
                .andExpect(status().isOk())
                //java.lang.AssertionError: No value at JSON path "$.error.code"
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)));
    }

    @Test
    void roleAdminCanCheckRegisterUpdateAndDisableTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolRequest(6, "roles/tools/check")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.valid", is(true)));

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolRequest(7, "roles/tools/register")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.registered", is(true)))
                .andExpect(jsonPath("$.result.version", is(1)));

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 8,
                                  "method": "roles/tools/update",
                                  "params": {
                                    "name": "architect.entries.copy",
                                    "patch": {
                                      "description": "List architect entries through a dynamic alias.",
                                      "enabled": true
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.updated", is(true)))
                .andExpect(jsonPath("$.result.previousVersion", is(1)))
                .andExpect(jsonPath("$.result.version", is(2)));

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 9,
                                  "method": "roles/tools/delete",
                                  "params": {
                                    "name": "architect.entries.copy",
                                    "mode": "disable"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.disabled", is(true)))
                .andExpect(jsonPath("$.result.version", is(3)));
    }

    @Test
    void roleAdminCanReconcileBundledToolRegistrationPhase() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 30,
                                  "method": "roles/tools/register",
                                  "params": {
                                    "phase": "bundle",
                                    "toolId": "helloworld.greet",
                                    "bundle": {
                                      "bundleId": "meshingress-tool-bundle"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.registered", is(true)))
                .andExpect(jsonPath("$.result.phase", is("bundle")))
                .andExpect(jsonPath("$.result.sourceKind", is("CLASSPATH_BUNDLE")))
                .andExpect(jsonPath("$.result.status", is("reconciled")))
                .andExpect(jsonPath("$.result.registeredFunctions[0]", is("helloworld.greet")));
    }

    @Test
    void bundleRegistrationFailsWhenToolIsNotOnClasspath() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 31,
                                  "method": "roles/tools/register",
                                  "params": {
                                    "phase": "bundle",
                                    "toolId": "missing.bundle.tool"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.INVALID_PARAMS)))
                .andExpect(jsonPath("$.error.data.errorCode", is("BUNDLE_TOOL_NOT_PRESENT")));
    }

    @Test
    void nativeRegistrationIsDisabledForHttpByDefault() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 32,
                                  "method": "roles/tools/register",
                                  "params": {
                                    "phase": "native",
                                    "toolId": "meshingress.runtime.info"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code", is(JsonRpcErrorCodes.FORBIDDEN)))
                .andExpect(jsonPath("$.error.data.errorCode", is("TOOL_REGISTRATION_PHASE_DISABLED")));
    }

    @Test
    void reservedMcpMethodsHaveMvpResponses() throws Exception {
        mockMvc.perform(get("/mcp"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/mcp"))
                .andExpect(status().isAccepted());
    }

    private String toolRequest(int id, String method) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "%s",
                  "params": {
                    "tool": {
                      "name": "architect.entries.copy",
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
                    }
                  }
                }
                """.formatted(id, method);
    }
}
