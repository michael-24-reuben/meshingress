package dev.mrk.meshingress.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.qbittorrent.username=test-user",
        "meshingress.qbittorrent.password=test-password",
        "meshingress.qbittorrent.job-store=target/qbittorrent-mvc-jobs.json"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpQbittorrentToolMvcTests {
    @Autowired private MockMvc mockMvc;

    @Test
    void toolsListIncludesTheComprehensiveQbittorrentJobSurface() throws Exception {
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content("""
                {"jsonrpc":"2.0","id":701,"method":"tools/list","params":{}}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItems(
                        "qbittorrent.health", "qbittorrent.add", "qbittorrent.list", "qbittorrent.status",
                        "qbittorrent.files", "qbittorrent.pause", "qbittorrent.resume", "qbittorrent.recheck",
                        "qbittorrent.reannounce", "qbittorrent.set-download-limit", "qbittorrent.set-upload-limit",
                        "qbittorrent.set-share-limits", "qbittorrent.set-sequential-download",
                        "qbittorrent.set-file-priority", "qbittorrent.collect", "qbittorrent.cancel", "qbittorrent.remove")));
    }
}
