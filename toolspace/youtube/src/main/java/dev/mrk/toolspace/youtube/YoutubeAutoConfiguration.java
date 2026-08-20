package dev.mrk.toolspace.youtube;

import dev.mrk.toolspace.youtube.catalog.YoutubeCapabilityCatalog;
import dev.mrk.toolspace.youtube.data.YoutubeDataApiClient;
import dev.mrk.toolspace.youtube.data.YoutubeDataApiProperties;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(YoutubeDataApiProperties.class)
public class YoutubeAutoConfiguration {
    @Bean
    YoutubeManifest youtubeManifest() {
        return new YoutubeManifest();
    }

    @Bean
    YoutubeCapabilityCatalog youtubeCapabilityCatalog() {
        return new YoutubeCapabilityCatalog();
    }

    @Bean
    YoutubeDataApiClient youtubeDataApiClient(ObjectMapper objectMapper, YoutubeDataApiProperties properties) {
        return new YoutubeDataApiClient(
                objectMapper,
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.requestTimeoutMs())).build()
        );
    }

    @Bean
    YoutubeCatalogTool youtubeCatalogTool(
            ObjectMapper objectMapper,
            YoutubeCapabilityCatalog catalog,
            McpToolMetadata mcpToolMetadata,
            YoutubeManifest manifest
    ) {
        mcpToolMetadata.registerManifest(YoutubeCatalogTool.class, manifest);
        return new YoutubeCatalogTool(objectMapper, catalog);
    }

    @Bean
    YoutubeTool youtubeTool(
            YoutubeDataApiClient dataApiClient,
            McpToolMetadata mcpToolMetadata,
            YoutubeManifest manifest
    ) {
        mcpToolMetadata.registerManifest(YoutubeTool.class, manifest);
        return new YoutubeTool(dataApiClient);
    }
}
