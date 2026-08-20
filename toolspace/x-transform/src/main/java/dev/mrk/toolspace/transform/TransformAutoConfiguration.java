package dev.mrk.toolspace.transform;

import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(TransformProperties.class)
public class TransformAutoConfiguration {
    @Bean TransformManifest transformManifest() { return new TransformManifest(); }
    @Bean TransformBackend transformBackend(ObjectMapper objectMapper, TransformProperties properties) { return new TransformBackend(objectMapper, properties); }
    @Bean TransformTool transformTool(ObjectMapper objectMapper, TransformBackend backend, McpToolMetadata metadata, TransformManifest manifest) {
        metadata.registerManifest(TransformTool.class, manifest);
        return new TransformTool(objectMapper, backend);
    }
}
