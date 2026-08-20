package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.controller.roles.registration.ToolContributionActivationMode;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolContributionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpToolContributionApiTests {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ToolContributionStore contributionStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("meshingress.repository.root", () -> tempDir.resolve("repository").toString());
        registry.add("meshingress.repository.runtime-registration-store-path", () -> "runtime/tool-registrations.json");
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("repository/sql/contribution-api"));
    }

    @Test
    void adminCanListAndChangePersistedContributionPrecedence() throws Exception {
        contributionStore.reserve(new ToolContributionRecord("extension", "youtube", 20,
                ToolContributionActivationMode.REQUIRES_PRIMARY, "reserved", OffsetDateTime.parse("2026-08-07T00:00:00Z"), "extension-module"));

        mockMvc.perform(post("/mcp").header("Authorization", "Bearer dev-admin")
                        .contentType("application/json")
                        .content("""
                                {"jsonrpc":"2.0","id":1,"method":"roles/tools/contributions/update","params":{
                                  "toolId":"extension","namespace":"youtube","precedence":5,"activationMode":"CONTRIBUTOR_WITH_FALLBACK"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.updated", is(true)))
                .andExpect(jsonPath("$.result.contribution.precedence", is(5)))
                .andExpect(jsonPath("$.result.contribution.activationMode", is("CONTRIBUTOR_WITH_FALLBACK")));

        mockMvc.perform(post("/mcp").header("Authorization", "Bearer dev-admin")
                        .contentType("application/json")
                        .content("""
                                {"jsonrpc":"2.0","id":2,"method":"roles/tools/contributions/list","params":{"namespace":"youtube"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contributions[0].toolId", is("extension")))
                .andExpect(jsonPath("$.result.contributions[0].precedence", is(5)))
                .andExpect(jsonPath("$.result.conflicts").isArray());
    }
}
