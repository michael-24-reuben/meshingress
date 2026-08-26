package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(YtDlpProperties.class)
public class YtDlpAutoConfiguration {
    @Bean
    YtDlpManifest ytDlpManifest() { return new YtDlpManifest(); }

    @Bean
    YtDlpBackend ytDlpBackend(ObjectMapper objectMapper, YtDlpProperties properties) {
        return new YtDlpBackend(objectMapper, properties);
    }

    @Bean
    YtDlpTool ytDlpTool(YtDlpBackend backend, McpToolMetadata metadata, YtDlpManifest manifest) {
        metadata.registerManifest(YtDlpTool.class, manifest);
        return new YtDlpTool(backend);
    }
}
