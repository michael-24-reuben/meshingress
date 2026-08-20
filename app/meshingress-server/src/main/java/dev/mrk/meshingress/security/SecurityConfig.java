package dev.mrk.meshingress.security;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MeshingressProperties properties,
            McpOAuthAuthenticationEntryPoint mcpAuthenticationEntryPoint
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(McpOAuthResourceMetadata.PATH, "/actuator/health").permitAll();
                    if (properties.security().oidc().enabled() && properties.security().requireAuthentication()) {
                        authorize.requestMatchers("/mcp", "/mcp/**").authenticated();
                    }
                    if (properties.security().oidc().enabled()) {
                        authorize.requestMatchers("/api/v1/auth/profile/**").authenticated();
                    }
                    authorize.anyRequest().permitAll();
                });
        if (properties.security().oidc().enabled()) {
            http.oauth2ResourceServer(resourceServer -> resourceServer
                    .authenticationEntryPoint(mcpAuthenticationEntryPoint)
                    .jwt(Customizer.withDefaults()));
        }
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "meshingress.security.oidc", name = "enabled", havingValue = "true")
    JwtDecoder mcpJwtDecoder(MeshingressProperties properties) {
        MeshingressProperties.Security.Oidc oidc = properties.security().oidc();
        if (oidc.issuerUri().isBlank() || oidc.audience().isBlank()) {
            throw new IllegalStateException("meshingress.security.oidc.issuer-uri and audience are required when OIDC is enabled");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(oidc.issuerUri()).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(oidc.audience())
                ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "JWT audience is not accepted by this MCP server", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(oidc.issuerUri()), audience));
        return decoder;
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
                "X-Mcp-Session-Id",
                "X-Request-Id"
        ));
        mcpCors.setExposedHeaders(List.of(
                "Mcp-Session-Id",
                "X-Mcp-Session-Id",
                "X-Request-Id"
        ));
        mcpCors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/mcp", mcpCors);
        CorsConfiguration workflowCors = new CorsConfiguration(mcpCors);
        // WebSocket upgrades begin with an HTTP GET before switching protocols.
        workflowCors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        source.registerCorsConfiguration("/api/v1/workflows/**", workflowCors);
        CorsConfiguration moduleCatalogCors = new CorsConfiguration(mcpCors);
        moduleCatalogCors.setAllowedMethods(List.of("GET", "OPTIONS"));
        source.registerCorsConfiguration("/api/v1/tool-modules/**", moduleCatalogCors);
        CorsConfiguration nativeLoginCors = new CorsConfiguration(mcpCors);
        nativeLoginCors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        source.registerCorsConfiguration("/api/v1/auth/**", nativeLoginCors);
        return source;
    }
}
