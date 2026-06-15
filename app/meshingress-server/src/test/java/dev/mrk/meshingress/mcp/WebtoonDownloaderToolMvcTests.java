package dev.mrk.meshingress.mcp;

import dev.mrk.toolspace.webtoon.WebtoonDownloaderCommandResult;
import dev.mrk.toolspace.webtoon.WebtoonDownloaderRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "meshingress.architect.root=../../architect",
        "meshingress.voicebox.base-url=http://127.0.0.1:1",
        "meshingress.webtoon-downloader.command=fake-webtoon-downloader",
        "meshingress.webtoon-downloader.output-root=${java.io.tmpdir}/meshingress-webtoon-test",
        "meshingress.webtoon-downloader.max-concurrent-chapters=2",
        "meshingress.webtoon-downloader.max-concurrent-pages=7"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebtoonDownloaderToolMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void toolsListIncludesWebtoonDownloaderFunctions() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 501,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("webtoon.inspect")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("webtoon.export_metadata")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("webtoon.download_series")));
    }

    @Test
    void exportMetadataRunsFakeCliWithinOutputRoot() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 502,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "webtoon.export_metadata",
                                    "arguments": {
                                      "url": "https://www.webtoons.com/en/fantasy/example/list?title_no=1234",
                                      "outputSubdirectory": "sample-export",
                                      "exportFormat": "json",
                                      "start": 1,
                                      "end": 2,
                                      "concurrentChapters": 99,
                                      "concurrentPages": 99
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(false)))
                .andExpect(jsonPath("$.result.structuredContent.ok", is(true)))
                .andExpect(jsonPath("$.result.structuredContent.command", hasItem("--export-metadata")))
                .andExpect(jsonPath("$.result.structuredContent.command", hasItem("--concurrent-chapters")))
                .andExpect(jsonPath("$.result.structuredContent.command", hasItem("2")))
                .andExpect(jsonPath("$.result.structuredContent.command", hasItem("--concurrent-pages")))
                .andExpect(jsonPath("$.result.structuredContent.command", hasItem("7")))
                .andExpect(jsonPath("$.result.structuredContent.outputFiles[0]", is("metadata.json")));
    }

    @Test
    void rejectsPathTraversalBeforeRunningCli() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 503,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "webtoon.download_series",
                                    "arguments": {
                                      "url": "https://www.webtoons.com/en/fantasy/example/list?title_no=1234",
                                      "outputSubdirectory": "../outside"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError", is(true)))
                .andExpect(jsonPath("$.result.structuredContent.message", containsString("inside the configured output root")));
    }

    @TestConfiguration
    static class FakeRunnerConfig {
        @Bean
        WebtoonDownloaderRunner fakeWebtoonDownloaderRunner() {
            return (List<String> command, Path workingDirectory, Duration timeout) -> {
                int outIndex = command.indexOf("--out");
                if (outIndex >= 0 && outIndex + 1 < command.size()) {
                    Path outputDirectory = Path.of(command.get(outIndex + 1));
                    Files.createDirectories(outputDirectory);
                    Files.writeString(outputDirectory.resolve("metadata.json"), "{\"title\":\"Example\"}");
                }
                if (command.contains("--version")) {
                    return new WebtoonDownloaderCommandResult(0, "webtoon-downloader 2.3.1", "", false, command);
                }
                return new WebtoonDownloaderCommandResult(0, "fake export complete", "", false, command);
            };
        }
    }
}
