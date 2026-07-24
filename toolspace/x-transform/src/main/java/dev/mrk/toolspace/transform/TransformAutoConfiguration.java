package dev.mrk.toolspace.transform;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(TransformProperties.class)
public class TransformAutoConfiguration {
    @Bean TransformBackend transformBackend(ObjectMapper objectMapper, TransformProperties properties) { return new TransformBackend(objectMapper, properties); }
    @Bean TransformTool transformTool(ObjectMapper objectMapper, TransformBackend backend) { return new TransformTool(objectMapper, backend); }
}
