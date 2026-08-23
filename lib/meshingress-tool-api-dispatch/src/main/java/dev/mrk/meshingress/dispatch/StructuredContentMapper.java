package dev.mrk.meshingress.dispatch;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

/** Serializes typed structured content into the stable Meshingress wire envelope. */
public final class StructuredContentMapper {
    private StructuredContentMapper() {}

    public static ObjectNode toJson(ObjectMapper objectMapper, StructuredContent content) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(content, "content must not be null");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("kind", content.kind());
        root.put("schema", content.schema());
        root.put("version", content.version());

        root.set("data", content.data(objectMapper));

        return root;
    }

    public static ObjectNode extraNamespace(ObjectMapper objectMapper, String namespace) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.set(namespace, objectMapper.createObjectNode());
        return root;
    }
}
