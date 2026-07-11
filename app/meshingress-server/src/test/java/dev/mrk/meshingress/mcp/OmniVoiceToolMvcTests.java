package dev.mrk.meshingress.mcp;

import dev.mrk.toolspace.omnivoice.OmniVoiceCommandResult;
import dev.mrk.toolspace.omnivoice.OmniVoiceRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.voicebox.base-url=http://127.0.0.1:1",
        "meshingress.omnivoice.command=fake-omnivoice --stdin",
        "meshingress.omnivoice.output-root=${java.io.tmpdir}/meshingress-omnivoice-test/outputs",
        "meshingress.omnivoice.reference-root=${java.io.tmpdir}/meshingress-omnivoice-test/reference-audio",
        "meshingress.omnivoice.voice-asset-root=${java.io.tmpdir}/meshingress-omnivoice-test/voice-assets",
        "meshingress.omnivoice.provider-downloads-enabled=false",
        "meshingress.omnivoice.model-pull-enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OmniVoiceToolMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void toolsListIncludesOmniVoiceFunctions() throws Exception {
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
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.generate")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.design")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.clone")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.models.status")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.models.pull")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.voices.import")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.voices.providers")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.voices.download")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("omnivoice.runtime.health")));
    }

    @Test
    void designInvokesConfiguredJsonWrapper() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 602,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "omnivoice.design",
                                    "arguments": {
                                      "text": "This is a controlled test.",
                                      "instruct": "female, low pitch, british accent",
                                      "outputPath": "designed.wav",
                                      "speed": 1.1,
                                      "numStep": 16
                                    }
                                  }
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.data.ok", is(true)))
                .andExpect(jsonPath("$.result.structuredContent.data.request.operation", is("design")))
                .andExpect(jsonPath("$.result.structuredContent.data.request.instruct", is("female, low pitch, british accent")))
                .andExpect(jsonPath("$.result.structuredContent.data.wrapperResponse.operation", is("design")))
                .andExpect(jsonPath("$.result.structuredContent.data.wrapperResponse.outputPath", startsWith(System.getProperty("java.io.tmpdir"))))
                .andExpect(jsonPath("$.result.structuredContent.data.commandResult.command[0]", is("fake-omnivoice")));
    }

    @Test
    void rejectsOutputPathTraversalBeforeRunningWrapper() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 603,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "omnivoice.generate",
                                    "arguments": {
                                      "text": "Path traversal test.",
                                      "outputPath": "../outside.wav"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(true)))
                .andExpect(jsonPath("$.result._meta.errorCode", is("OMNIVOICE_GENERATE_FAILED")))
                .andExpect(jsonPath("$.result.structuredContent.data.message", is("outputPath must stay inside the configured OmniVoice output root")));
    }

    @Test
    void providerDownloadIsDisabledByDefault() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 604,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "omnivoice.voices.download",
                                    "arguments": {
                                      "providerId": "example",
                                      "voiceId": "demo"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(true)))
                .andExpect(jsonPath("$.result._meta.errorCode", is("OMNIVOICE_PROVIDER_DOWNLOAD_DISABLED")))
                .andExpect(jsonPath("$.result.structuredContent.data.details.providerDownloadsEnabled", is(false)));
    }

    @TestConfiguration
    static class FakeRunnerConfig {
        @Bean
        OmniVoiceRunner fakeOmniVoiceRunner(ObjectMapper objectMapper) {
            return (List<String> command, JsonNode request, Path workingDirectory, Duration timeout) -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("status", "ok");
                response.put("operation", request.path("operation").asText());
                response.put("outputPath", request.path("outputPath").asText());
                response.put("sampleRate", 24000);
                return new OmniVoiceCommandResult(0, objectMapper.writeValueAsString(response), "", false, command);
            };
        }
    }
}
