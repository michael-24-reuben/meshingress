package dev.mrk.meshingress.security;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(MeshingressProperties properties) {
        CorsConfiguration mcpCors = new CorsConfiguration();
        mcpCors.setAllowedOriginPatterns(properties.mcp().websocket().allowedOrigins());
        mcpCors.setAllowedMethods(List.of("POST", "OPTIONS"));
        mcpCors.setAllowedHeaders(List.of(
                "Accept",
                "Authorization",
                "Content-Type",
                "Mcp-Session-Id",
                "X-Auth-Token",
                "X-Mcp-Admin",
                "X-Mcp-Role",
                "X-Mcp-Session-Id",
                "X-Request-Id",
                "X-Secret-Key"
        ));
        mcpCors.setExposedHeaders(List.of(
                "Mcp-Session-Id",
                "X-Mcp-Session-Id",
                "X-Request-Id"
        ));
        mcpCors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/mcp", mcpCors);
        return source;
    }
}
