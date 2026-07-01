package dev.mrk.toolspace.videoloop;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class VideoLoopToolAutoConfiguration {

    @Bean
    VideoLoopTool videoLoopTool(ObjectMapper objectMapper) {
        return new VideoLoopTool(objectMapper);
    }
}
