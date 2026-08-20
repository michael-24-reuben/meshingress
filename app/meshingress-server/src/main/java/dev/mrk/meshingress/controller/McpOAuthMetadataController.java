package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.security.McpOAuthResourceMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RFC 9728 metadata used by OAuth-aware MCP clients to discover the authority. */
@RestController
public final class McpOAuthMetadataController {
    private final McpOAuthResourceMetadata metadata;

    public McpOAuthMetadataController(McpOAuthResourceMetadata metadata) {
        this.metadata = metadata;
    }

    @GetMapping(value = McpOAuthResourceMetadata.PATH, produces = "application/json")
    public Map<String, Object> metadata() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resource", metadata.resource());
        if (!metadata.authorizationServer().isBlank()) {
            result.put("authorization_servers", List.of(metadata.authorizationServer()));
        }
        return Map.copyOf(result);
    }
}
