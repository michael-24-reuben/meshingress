package dev.mrk.meshingress.api.result;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

public final class ResultContent {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_MIME = "mime";

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private final String type;
    private final MimeType mimeType;
    private final JsonNode value;

    private ResultContent(String type, MimeType mimeType, JsonNode value) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.mimeType = mimeType;
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static ResultContent text(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new ResultContent(
                TYPE_TEXT,
                MimeTypeUtils.TEXT_PLAIN,
                DEFAULT_OBJECT_MAPPER.valueToTree(value)
        );
    }

    public static ResultContent text(JsonNode value) {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.isString()) {
            throw new IllegalArgumentException("text content requires a textual JsonNode");
        }
        return new ResultContent(TYPE_TEXT, MimeTypeUtils.TEXT_PLAIN, value);
    }

    public static ResultContent object(JsonNode value) {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.isObject()) {
            throw new IllegalArgumentException("object content requires an object JsonNode");
        }
        return json(value);
    }

    public static ResultContent array(JsonNode value) {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.isArray()) {
            throw new IllegalArgumentException("array content requires an array JsonNode");
        }
        return json(value);
    }

    public static ResultContent number(Number value) {
        Objects.requireNonNull(value, "value must not be null");
        return json(DEFAULT_OBJECT_MAPPER.valueToTree(value));
    }

    public static ResultContent bool(boolean value) {
        return json(DEFAULT_OBJECT_MAPPER.valueToTree(value));
    }

    public static ResultContent json(JsonNode value) {
        return mime(MimeTypeUtils.APPLICATION_JSON, value);
    }

    public static ResultContent mime(MimeType mimeType, JsonNode value) {
        Objects.requireNonNull(mimeType, "mimeType must not be null");
        Objects.requireNonNull(value, "value must not be null");

        String type = MimeTypeUtils.APPLICATION_JSON.includes(mimeType)
                ? TYPE_JSON
                : TYPE_MIME;

        return new ResultContent(type, mimeType, value);
    }

    public String type() {
        return type;
    }

    public MimeType mimeType() {
        return mimeType;
    }

    public JsonNode value() {
        return value;
    }

    public ObjectNode toJson() {
        return toJson(DEFAULT_OBJECT_MAPPER);
    }

    public ObjectNode toJson(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);

        if (TYPE_TEXT.equals(type)) {
            node.set("text", value);
            return node;
        }

        if (mimeType != null) {
            node.put("mimeType", mimeType.toString());
        }

        node.set("data", value);
        return node;
    }
}