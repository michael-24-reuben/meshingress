package dev.mrk.toolspace.youtube;

import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import dev.mrk.toolspace.youtube.catalog.YoutubeCapabilityCatalog;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeManifestTests {
    @Test
    void declaresBlankProductionCredentialPropertiesAndRegistersThemWithTheToolMetadata() {
        YoutubeManifest manifest = new YoutubeManifest();
        McpToolMetadata metadata = new McpToolMetadata();
        var configuration = new YoutubeAutoConfiguration();
        configuration.youtubeCatalogTool(
                new ObjectMapper(),
                new YoutubeCapabilityCatalog(),
                metadata,
                manifest
        );

        assertThat(manifest.metadata().namespace()).isEqualTo("youtube");
        assertThat(manifest.properties()).allSatisfy(property -> assertThat(property.defaultValue()).isBlank());
        assertThat(manifest.properties())
                .extracting(property -> property.name())
                .containsExactly(
                        "meshingress.youtube.api-key",
                        "meshingress.youtube.oauth.client-id",
                        "meshingress.youtube.oauth.client-secret",
                        "meshingress.youtube.oauth.redirect-uri"
                );
        assertThat(manifest.properties())
                .filteredOn(property -> property.name().equals("meshingress.youtube.api-key")
                        || property.name().equals("meshingress.youtube.oauth.client-secret"))
                .allSatisfy(property -> {
                    assertThat(property.secret()).isTrue();
                    assertThat(property.valueType()).isEqualTo("secret");
                });
        assertThat(metadata.toolProperties(YoutubeCatalogTool.class))
                .extracting(property -> property.name())
                .containsExactly(
                        "meshingress.youtube.api-key",
                        "meshingress.youtube.oauth.client-id",
                        "meshingress.youtube.oauth.client-secret",
                        "meshingress.youtube.oauth.redirect-uri"
                );
    }
}
