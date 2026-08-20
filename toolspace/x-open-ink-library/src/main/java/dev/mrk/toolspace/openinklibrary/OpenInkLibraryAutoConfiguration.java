package dev.mrk.toolspace.openinklibrary;

import dev.mrk.toolspace.openinklibrary.source.toonverse.*;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.http.HttpClient;

@AutoConfiguration
public class OpenInkLibraryAutoConfiguration {
    private static final String TOONVERSE_PROFILE = "/open-ink-library/toonverse.yaml";

    @Bean
    OpenInkLibraryManifest openInkLibraryManifest() {
        return new OpenInkLibraryManifest();
    }

    @Bean
    ToonverseSourceConfiguration toonverseSourceConfiguration() {
        try (InputStream input = OpenInkLibraryAutoConfiguration.class.getResourceAsStream(TOONVERSE_PROFILE)) {
            if (input == null) {
                throw new SourceException("INVALID_SOURCE_CONFIGURATION", "Missing bundled Toonverse source profile.");
            }
            return new ToonverseSourceConfigurationLoader().load(input);
        } catch (java.io.IOException exception) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "Unable to read bundled Toonverse source profile.", exception);
        }
    }

    @Bean
    ToonverseMapper toonverseMapper() {
        return new ToonverseMapper();
    }

    @Bean
    ToonverseClient toonverseClient(ObjectMapper objectMapper, ToonverseSourceConfiguration configuration) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(configuration.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new ToonverseClient(httpClient, objectMapper, configuration);
    }

    @Bean
    ToonverseImageBookSource toonverseImageBookSource(
            ToonverseClient client,
            ToonverseMapper mapper,
            ToonverseSourceConfiguration configuration
    ) {
        return new ToonverseImageBookSource(client, mapper, configuration);
    }

    @Bean
    ToonverseTool toonverseTool(
            ToonverseClient client,
            ObjectMapper objectMapper,
            ObjectProvider<ToolStorageService> storage,
            McpToolMetadata metadata,
            OpenInkLibraryManifest manifest
    ) {
        metadata.registerManifest(TextBookTool.class, manifest);
        metadata.registerManifest(ImageBookTool.class, manifest);
        metadata.registerManifest(ToonverseTool.class, manifest);
        return new ToonverseTool(client, objectMapper, storage.getIfAvailable());
    }
}
