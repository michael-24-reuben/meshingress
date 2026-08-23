package dev.mrk.toolspace.youtube;

import dev.mrk.toolspace.youtube.catalog.YoutubeCapabilityCatalog;
import dev.mrk.toolspace.youtube.data.YoutubeDataApiClient;
import dev.mrk.toolspace.youtube.data.YoutubeDataApiProperties;
import dev.mrk.toolspace.youtube.data.YoutubeResourceArgs;
import dev.mrk.toolspace.youtube.data.YoutubeSearchArgs;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeToolTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void capabilitiesDistinguishCallableFunctionsFromPlannedFollowUps() {
        var result = catalogTool().capabilities(null);

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent().orElseThrow().path("kind").asString()).isEqualTo("data.records");
        assertThat(result.toJson(objectMapper).at("/structuredContent/data/records/2/function").asString()).isEqualTo("youtube.data.search");
        assertThat(result.toJson(objectMapper).at("/structuredContent/data/records/2/status").asString()).isEqualTo("available");
        assertThat(result.toJson(objectMapper).at("/structuredContent/data/records/0/status").asString()).isEqualTo("available");
        assertThat(result.toJson(objectMapper).at("/structuredContent/data/records/6/status").asString()).isEqualTo("planned");
    }

    @Test
    void providersKeepTranscriptAndLocalMediaResponsibilitiesSeparate() {
        var result = catalogTool().providers(null);

        assertThat(result.isError()).isFalse();
        assertThat(result.toJson(objectMapper).toString()).contains("dedicated-transcript-provider", "media.ytdlp");
        assertThat(result.toJson(objectMapper).at("/structuredContent/data/records/1/status").asString()).isEqualTo("available");
    }

    @Test
    void publicDataFunctionsFailClosedUntilTheApiKeyIsConfigured() {
        var result = dataTool().search(new YoutubeSearchArgs("meshingress", null, null, null, null, null), null);

        assertThat(result.isError()).isTrue();
        assertThat(result.toJson(objectMapper).toString()).contains("YOUTUBE_API_KEY_NOT_CONFIGURED");
    }

    @Test
    void publicDataFunctionsRejectInvalidIdentifiersBeforeCallingTheApi() {
        var result = dataTool().videos(new YoutubeResourceArgs(List.of(" ")), null);

        assertThat(result.isError()).isTrue();
        assertThat(result.toJson(objectMapper).toString()).contains("INVALID_ARGUMENTS");
    }

    private YoutubeCatalogTool catalogTool() {
        return new YoutubeCatalogTool(objectMapper, new YoutubeCapabilityCatalog());
    }

    private YoutubeTool dataTool() {
        return new YoutubeTool(new YoutubeDataApiClient(
                objectMapper,
                new YoutubeDataApiProperties("", null, null),
                HttpClient.newHttpClient()
        ));
    }
}
