package dev.mrk.meshingress.security;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

/** Builds the RFC 9728 protected-resource identifiers from deployment configuration. */
@Component
public final class McpOAuthResourceMetadata {
    public static final String PATH = "/.well-known/oauth-protected-resource/mcp";

    private final MeshingressProperties properties;

    public McpOAuthResourceMetadata(MeshingressProperties properties) {
        this.properties = properties;
    }

    public String resource() {
        return resolve("/mcp");
    }

    public String metadataUri() {
        return resolve(PATH);
    }

    public String authorizationServer() {
        return properties.security().oidc().issuerUri();
    }

    private String resolve(String path) {
        URI base = properties.identity().publicBaseUrl();
        return base.toString().replaceAll("/+$", "") + path;
    }
}
