package dev.mrk.toolspace.helloworld;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class HelloWorldToolAutoConfiguration {

    @Bean
    HelloWorldTool helloWorldTool(ObjectMapper objectMapper) {
        return new HelloWorldTool(objectMapper);
    }
}
