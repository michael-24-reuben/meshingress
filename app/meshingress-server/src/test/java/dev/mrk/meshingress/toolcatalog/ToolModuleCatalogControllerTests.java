package dev.mrk.meshingress.toolcatalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ToolModuleCatalogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolModuleCatalog catalog;

    @Test
    void exposesTheBundledPowerShellManifestAndItsDeclaredResources() throws Exception {
        String toolId = catalog.list().stream()
                .filter(entry -> entry.namespace().equals("powershell"))
                .findFirst()
                .orElseThrow()
                .toolId();

        mockMvc.perform(get("/api/v1/tool-modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.namespace == 'powershell')].source").value("classpath"))
                .andExpect(jsonPath("$.items[?(@.namespace == 'powershell')].toolId").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.startsWith("cp-"))))
                .andExpect(jsonPath("$.items[?(@.namespace == 'powershell')].icon.mediaType").value("image/svg+xml"));

        mockMvc.perform(get("/api/v1/tool-modules/{toolId}", toolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolId").value(toolId))
                .andExpect(jsonPath("$.namespace").value("powershell"))
                .andExpect(jsonPath("$.metadata.title").value("PowerShell CLI"))
                .andExpect(jsonPath("$.properties[0].name").value("meshingress.powershell.executable"))
                .andExpect(jsonPath("$.requirements[0].name").value("pwsh"))
                .andExpect(jsonPath("$.readme.mediaType").value("text/markdown"));

        mockMvc.perform(get("/api/v1/tool-modules/{toolId}/readme", toolId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().string(containsString("# PowerShell CLI")));

        mockMvc.perform(get("/api/v1/tool-modules/{toolId}/icon", toolId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    void servesDistinctIconsWhenBundledModulesDeclareNamespaceNamedResources() throws Exception {
        String powerShellToolId = catalog.list().stream()
                .filter(entry -> entry.namespace().equals("powershell"))
                .findFirst()
                .orElseThrow()
                .toolId();
        String youtubeToolId = catalog.list().stream()
                .filter(entry -> entry.namespace().equals("youtube"))
                .findFirst()
                .orElseThrow()
                .toolId();

        mockMvc.perform(get("/api/v1/tool-modules/{toolId}/icon", powerShellToolId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SVGxPWimdiY")));
        mockMvc.perform(get("/api/v1/tool-modules/{toolId}/icon", youtubeToolId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fill=\"red\"")));
    }

    @Test
    void toolsListLinksPowerShellFunctionsToThePublicModuleToolId() throws Exception {
        String toolId = catalog.list().stream()
                .filter(entry -> entry.namespace().equals("powershell"))
                .findFirst()
                .orElseThrow()
                .toolId();

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":31,"method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[?(@.name == 'powershell.cli.execute')].moduleToolId")
                        .value(org.hamcrest.Matchers.hasItem(toolId)));
    }
}
