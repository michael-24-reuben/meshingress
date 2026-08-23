package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "meshingress.architect.root=../../architect")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpOutputSchemaValidationMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void declaredOutputSchemaIsValidatedAtTheCommonToolExecutionBoundary() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":804,"method":"tools/call","params":{"name":"youtube.catalog.capabilities","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").doesNotExist())
                .andExpect(jsonPath("$.result._meta.meshingress.outputSchema.policy").value("warn"))
                .andExpect(jsonPath("$.result._meta.meshingress.outputSchema.validated").value(true))
                .andExpect(jsonPath("$.result._meta.meshingress.outputSchema.valid").value(true));
    }
}
