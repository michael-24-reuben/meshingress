package dev.mrk.toolspace.whatsapp;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class WhatsappCobaltToolAutoConfiguration {

    @Bean
    WhatsappCobaltSessionService whatsappCobaltSessionService(ObjectMapper objectMapper) {
        return new WhatsappCobaltSessionService(objectMapper);
    }

    @Bean
    WhatsappCobaltTool whatsappCobaltTool(WhatsappCobaltSessionService sessionService) {
        return new WhatsappCobaltTool(sessionService);
    }
}
