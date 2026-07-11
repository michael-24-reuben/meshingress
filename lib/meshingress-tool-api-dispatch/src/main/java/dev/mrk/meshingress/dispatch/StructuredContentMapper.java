package dev.mrk.meshingress.dispatch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;

/** Serializes typed structured content into the stable Meshingress wire envelope. */
public final class StructuredContentMapper {
    private static final List<String> ENVELOPE_FIELD_NAMES = List.of(
            "kind", "schema", "version"
    );

    private StructuredContentMapper() {}

    public static ObjectNode toJson(ObjectMapper objectMapper, StructuredContent content) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(content, "content must not be null");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("kind", content.kind());
        root.put("schema", content.schema());
        root.put("version", content.version());

        JsonNode data = objectMapper.valueToTree(content);
        if (data instanceof ObjectNode dataObject) {
            dataObject.remove(ENVELOPE_FIELD_NAMES);
            root.set("data", dataObject);
        } else {
            root.set("data", data);
        }

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
