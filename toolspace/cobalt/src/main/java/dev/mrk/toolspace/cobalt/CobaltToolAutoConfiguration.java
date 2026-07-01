package dev.mrk.toolspace.cobalt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
public class CobaltToolAutoConfiguration {

    @Bean
    CobaltTool cobaltTool(
            ObjectMapper objectMapper,
            @Value("${meshingress.cobalt.base-url:http://127.0.0.1:9000}") String baseUrl,
            @Value("${meshingress.cobalt.auth-header:}") String authHeader,
            @Value("${meshingress.cobalt.user-agent:Meshingress-Cobalt/0.1}") String userAgent
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return new CobaltTool(new CobaltClient(httpClient, objectMapper, baseUrl, authHeader, userAgent), objectMapper);
    }
}
