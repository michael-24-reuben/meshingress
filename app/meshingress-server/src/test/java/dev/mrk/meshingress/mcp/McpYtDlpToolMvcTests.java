package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "meshingress.architect.root=../../architect")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpYtDlpToolMvcTests {
    @Autowired private MockMvc mockMvc;

    @Test
    void toolsListIncludesTheLocalYtDlpFunctions() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":801,"method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItems(
                        "media.ytdlp.inspect",
                        "media.ytdlp.formats",
                        "media.ytdlp.subtitles-list",
                        "media.ytdlp.subtitles-download",
                        "media.ytdlp.thumbnails-list",
                        "media.ytdlp.thumbnails-download",
                        "media.ytdlp.download"
                )) )
                .andExpect(jsonPath("$.result.tools[*].name", not(hasItem("media.ytdlp.dump-json"))));
    }
}
