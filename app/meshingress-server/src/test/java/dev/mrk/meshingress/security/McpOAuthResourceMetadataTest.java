package dev.mrk.meshingress.security;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpOAuthResourceMetadataTest {
    @Test
    void derivesTheRfc9728ResourceAndMetadataUrisFromThePublicBaseUrl() {
        MeshingressProperties properties = new MeshingressProperties(
                new MeshingressProperties.Identity("meshingress", "node", "test", URI.create("https://mcp.example.test/"), "edge"),
                null, null, null, null,
                new MeshingressProperties.Security(true, "prod", true, false, "", false, true, true, true, true,
                        new MeshingressProperties.Security.Oidc(true, "https://accounts.google.com", "client.apps.googleusercontent.com", "tenant_id", "profile_id", "roles", "scope")),
                null, null, null, null, null
        );
        McpOAuthResourceMetadata metadata = new McpOAuthResourceMetadata(properties);

        assertEquals("https://mcp.example.test/mcp", metadata.resource());
        assertEquals("https://mcp.example.test/.well-known/oauth-protected-resource/mcp", metadata.metadataUri());
        assertEquals("https://accounts.google.com", metadata.authorizationServer());
    }

    @Test
    void unauthenticatedMcpRequestsReceiveTheResourceMetadataChallenge() throws Exception {
        MeshingressProperties properties = new MeshingressProperties(
                new MeshingressProperties.Identity("meshingress", "node", "test", URI.create("https://mcp.example.test"), "edge"),
                null, null, null, null, null, null, null, null, null, null
        );
        McpOAuthAuthenticationEntryPoint entryPoint = new McpOAuthAuthenticationEntryPoint(new McpOAuthResourceMetadata(properties));
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("missing bearer token"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer resource_metadata=\"https://mcp.example.test/.well-known/oauth-protected-resource/mcp\"",
                response.getHeader("WWW-Authenticate"));
    }
}
