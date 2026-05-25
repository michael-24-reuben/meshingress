package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.McpCallContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

@Component
public class McpCacheKeyGenerator {

    private final ObjectMapper objectMapper;

    public McpCacheKeyGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpCacheKey generate(
            McpCachePolicy policy,
            String toolId,
            String functionName,
            ObjectNode arguments,
            McpCallContext context
    ) {
        ObjectNode keyMaterial = objectMapper.createObjectNode();
        if (!policy.keyPrefix().isBlank()) {
            keyMaterial.put("keyPrefix", policy.keyPrefix());
        }
        if (policy.includeToolId()) {
            keyMaterial.put("toolId", toolId);
        }
        if (policy.includeFunctionName()) {
            keyMaterial.put("functionName", functionName);
        }
        if (policy.includePrincipal()) {
            keyMaterial.put("principal", context.authorizationHeader() == null ? "" : context.authorizationHeader());
        }
        if (policy.includeSession()) {
            keyMaterial.put("sessionId", context.sessionId() == null ? "" : context.sessionId());
        }
        keyMaterial.set("arguments", filteredArguments(policy, arguments));
        return new McpCacheKey(policy.namespace(), sha256(canonicalize(keyMaterial).toString()));
    }

    private ObjectNode filteredArguments(McpCachePolicy policy, ObjectNode arguments) {
        ObjectNode filtered = objectMapper.createObjectNode();
        Set<String> includes = new TreeSet<>(policy.includeArguments());
        Set<String> excludes = new TreeSet<>(policy.excludeArguments());
        boolean explicitOnly = !includes.isEmpty();

        arguments.properties().stream()
                .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
                .forEach(field -> {
                    String name = field.getKey();
                    if (explicitOnly && !includes.contains(name)) {
                        return;
                    }
                    if (excludes.contains(name)) {
                        return;
                    }
                    filtered.set(name, canonicalize(field.getValue()));
                });
        return filtered;
    }

    private JsonNode canonicalize(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode() || value.isValueNode()) {
            return value == null ? objectMapper.nullNode() : value.deepCopy();
        }
        if (value.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            value.forEach(item -> array.add(canonicalize(item)));
            return array;
        }
        if (value.isObject()) {
            ObjectNode object = objectMapper.createObjectNode();
            value.properties().stream()
                    .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
                    .forEach(field -> object.set(field.getKey(), canonicalize(field.getValue())));
            return object;
        }
        return value.deepCopy();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
