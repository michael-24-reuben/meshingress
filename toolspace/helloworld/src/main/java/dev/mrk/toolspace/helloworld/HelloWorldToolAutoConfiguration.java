package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class HelloWorldToolAutoConfiguration {

    @Bean
    HelloWorldManifest helloWorldManifest() {
        return new HelloWorldManifest();
    }

    @Bean
    HelloWorldTool helloWorldTool(McpToolMetadata mcpToolMetadata, HelloWorldManifest manifest) {
        mcpToolMetadata.registerManifest(HelloWorldTool.class, manifest);
        return new HelloWorldTool();
    }
}
