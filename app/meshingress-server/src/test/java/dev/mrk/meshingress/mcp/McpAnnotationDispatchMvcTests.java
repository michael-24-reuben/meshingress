package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.MeshingressApplication;
import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {
                MeshingressApplication.class,
                McpAnnotationDispatchMvcTests.TestDispatchConfig.class
        },
        properties = "meshingress.architect.root=../../architect"
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpAnnotationDispatchMvcTests {

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @Test
    void annotatedDispatchUsesNormalJsonRpcEnvelopeAndManualControllersStillWork() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 30,
                                  "method": "test/echo",
                                  "params": {
                                    "name": "annotation"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc", is("2.0")))
                .andExpect(jsonPath("$.id", is(30)))
                .andExpect(jsonPath("$.result.name", is("annotation")))
                .andExpect(jsonPath("$.result.method", is("test/echo")));

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 31,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[0].name", is("architect.entries.list")));
    }

    @Test
    void annotatedDispatchNotificationReturnsNoContent() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "method": "test/echo",
                                  "params": {
                                    "name": "notification"
                                  }
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestDispatchConfig {

        @Bean
        TestAnnotatedHandler testAnnotatedHandler() {
            return new TestAnnotatedHandler();
        }
    }

    @McpDispatchMapping("test")
    static class TestAnnotatedHandler {

        @McpDispatchMethod("echo")
        Map<String, Object> echo(
                @McpDispatchParam("name") String name,
                @McpDispatchParam("method") String method,
                McpCallContext context
        ) {
            return Map.of(
                    "name", name,
                    "method", method,
                    "requestIdPresent", context.requestId() != null
            );
        }
    }
}
