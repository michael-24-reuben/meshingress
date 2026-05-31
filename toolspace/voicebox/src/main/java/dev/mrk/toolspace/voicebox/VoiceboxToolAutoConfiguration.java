package dev.mrk.toolspace.voicebox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
public class VoiceboxToolAutoConfiguration {

    @Bean
    VoiceboxTool voiceboxTool(
            ObjectMapper objectMapper,
            @Value("${meshingress.voicebox.base-url:http://127.0.0.1:17493}") String baseUrl,
            @Value("${meshingress.voicebox.client-id:meshingress}") String clientId
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return new VoiceboxTool(new VoiceboxClient(httpClient, objectMapper, baseUrl, clientId), objectMapper);
    }
}
