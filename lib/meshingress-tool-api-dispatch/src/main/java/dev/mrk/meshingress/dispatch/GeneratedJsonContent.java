package dev.mrk.meshingress.dispatch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Parsed JSON represented with the standard Meshingress structured-content envelope.
 * <p>
 * Its kind describes only the root JSON shape. It deliberately does not declare a stable
 * field-level payload schema; callers that need that contract should use a domain-specific
 * {@link StructuredContent} implementation instead.
 */
public final class GeneratedJsonContent extends StructuredContent {

    private final JsonNode payload;

    public GeneratedJsonContent(JsonNode payload) {
        super(kindFor(payload));
        this.payload = Objects.requireNonNull(payload, "payload must not be null").deepCopy();
    }

    /** Returns a defensive copy of the parsed JSON payload. */
    public JsonNode payload() {
        return payload.deepCopy();
    }

    @Override
    public JsonNode data(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        return payload();
    }

    private static StructuredContentKind.GeneratedJson kindFor(JsonNode payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.isObject()) {
            return StructuredContentKind.GeneratedJson.OBJECT;
        }
        if (payload.isArray()) {
            return StructuredContentKind.GeneratedJson.ARRAY;
        }
        if (payload.isTextual()) {
            return StructuredContentKind.GeneratedJson.STRING;
        }
        if (payload.isNumber()) {
            return StructuredContentKind.GeneratedJson.NUMBER;
        }
        if (payload.isBoolean()) {
            return StructuredContentKind.GeneratedJson.BOOLEAN;
        }
        if (payload.isNull()) {
            return StructuredContentKind.GeneratedJson.NULL;
        }
        throw new IllegalArgumentException("payload must be a concrete JSON value");
    }
}
