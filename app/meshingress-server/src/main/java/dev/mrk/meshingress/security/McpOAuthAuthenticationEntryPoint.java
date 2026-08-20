package dev.mrk.meshingress.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Emits the MCP resource metadata location with an OAuth Bearer challenge. */
@Component
public final class McpOAuthAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final McpOAuthResourceMetadata metadata;

    public McpOAuthAuthenticationEntryPoint(McpOAuthResourceMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"%s\"".formatted(metadata.metadataUri()));
    }
}
