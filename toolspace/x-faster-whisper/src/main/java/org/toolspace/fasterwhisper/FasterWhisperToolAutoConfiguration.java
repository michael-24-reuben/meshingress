package org.toolspace.fasterwhisper;

import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(FasterWhisperManifestProperties.class)
public class FasterWhisperToolAutoConfiguration {

    @Bean
    FasterWhisperManifest fasterWhisperManifest() {
        return new FasterWhisperManifest();
    }
    @Bean
    FasterWhisperBridgeScript fasterWhisperBridgeScript() {
        return new FasterWhisperBridgeScript();
    }

    @Bean
    FasterWhisperTool fasterWhisperTool(
            ObjectMapper objectMapper,
            FasterWhisperManifestProperties properties,
            FasterWhisperBridgeScript bridgeScript,
            McpToolMetadata mcpToolMetadata,
            FasterWhisperManifest manifest
    ) {
        mcpToolMetadata.registerManifest(FasterWhisperTool.class, manifest);
        return new FasterWhisperTool(objectMapper, properties, bridgeScript);
    }
}
