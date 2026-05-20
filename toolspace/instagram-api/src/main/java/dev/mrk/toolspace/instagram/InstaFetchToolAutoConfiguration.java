package dev.mrk.toolspace.instagram;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class InstaFetchToolAutoConfiguration {

    @Bean
    InstaFetchTool instaFetchTool() {
        return new InstaFetchTool();
    }
}
