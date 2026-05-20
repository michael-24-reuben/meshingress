package dev.mrk.meshingress.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${meshingress.mcp.websocket.path:/mcp/ws}") String mcpWebSocketPath
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/mcp",
                                mcpWebSocketPath,
                                "/actuator/health",
                                "/api/v1/auth/bootstrap/admin"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
