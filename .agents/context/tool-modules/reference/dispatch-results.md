
```java
package dev.mrk.meshingress.api.result;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DispatchExecutionResult {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    private final List<ResultContent> content;
    private JsonNode structuredContent;
    private boolean error;
    private String status;
    private String summary;
    private String errorCode;
    private String errorMessage;
    private ObjectNode meta;
    private DispatchExecutionResult() {
        this.content = new ArrayList<>();
    }
    @Contract(value = " -> new", pure = true)
    public static @NonNull DispatchExecutionResult create() {
        return new DispatchExecutionResult();
    }
    @Contract(" -> new")
    public static @NonNull Builder builder() {
        return new Builder();
    }
    public DispatchExecutionResult appendContent(ResultContent content) {
        this.content.add(Objects.requireNonNull(content, "content must not be null"));
        return this;
    }
    public DispatchExecutionResult setStructuredContent(JsonNode structuredContent) {
        this.structuredContent = Objects.requireNonNull(structuredContent, "structuredContent must not be null");
        return this;
    }
    public DispatchExecutionResult setError(boolean error) {
        this.error = error;
        return this;
    }
    public DispatchExecutionResult markError() {
        this.error = true;
        return this;
    }
    public DispatchExecutionResult markError(String errorCode, String errorMessage) {
        this.error = true;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        return this;
    }
    public DispatchExecutionResult setStatus(String status) {
        this.status = status;
        return this;
    }
    public DispatchExecutionResult setSummary(String summary) {
        this.summary = summary;
        return this;
    }
    public DispatchExecutionResult setMeta(ObjectNode meta) {
        this.meta = Objects.requireNonNull(meta, "meta must not be null");
        return this;
    }
    @Contract(pure = true)
    public @NonNull Optional<JsonNode> structuredContent() {
        return Optional.ofNullable(structuredContent);
    }
    @Contract(pure = true)
    public @NonNull @Unmodifiable List<ResultContent> content() {
        return List.copyOf(content);
    }
    public boolean isError() {
        return error;
    }
    public ObjectNode toJson() {
        return toJson(DEFAULT_OBJECT_MAPPER);
    }
    public ObjectNode toJson(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode contentArray = objectMapper.createArrayNode();
        for (ResultContent item : content) {
            contentArray.add(item.toJson(objectMapper));
        }
        root.set("content", contentArray);

        if (structuredContent != null) {
            root.set("structuredContent", structuredContent);
        }

        if (error) {
            root.put("isError", true);
        }

        ObjectNode metaNode = buildMeta(objectMapper);
        if (!metaNode.isEmpty()) {
            root.set("_meta", metaNode);
        }

        return root;
    }
    private ObjectNode buildMeta(ObjectMapper objectMapper) {
        ObjectNode metaNode = meta == null ? objectMapper.createObjectNode() : meta.deepCopy();
        if (status != null && !status.isBlank()) {metaNode.put("status", status);}
        if (summary != null && !summary.isBlank()) {metaNode.put("summary", summary);}
        if (errorCode != null && !errorCode.isBlank()) {metaNode.put("errorCode", errorCode);}
        if (errorMessage != null && !errorMessage.isBlank()) {metaNode.put("errorMessage", errorMessage);}
        if (!metaNode.has("generatedAt")) {metaNode.put("generatedAt", Instant.now().toString());}
        return metaNode;
    }
    public static final class Builder {
        private final DispatchExecutionResult result = new DispatchExecutionResult();
        private Builder() {}
        public Builder appendContent(ResultContent content) {
            result.appendContent(content);
            return this;
        }
        public Builder content(ResultContent content) {
            return appendContent(content);
        }
        public Builder text(String value) {
            result.appendContent(ResultContent.text(value));
            return this;
        }
        public Builder json(JsonNode value) {
            result.appendContent(ResultContent.json(value));
            return this;
        }
        public Builder object(JsonNode value) {
            result.appendContent(ResultContent.object(value));
            return this;
        }
        public Builder array(JsonNode value) {
            result.appendContent(ResultContent.array(value));
            return this;
        }
        public Builder structuredContent(JsonNode structuredContent) {
            result.setStructuredContent(structuredContent);
            return this;
        }
        public Builder error(boolean error) {
            result.setError(error);
            return this;
        }
        public Builder error(String errorCode, String errorMessage) {
            result.markError(errorCode, errorMessage);
            return this;
        }
        public Builder status(String status) {
            result.setStatus(status);
            return this;
        }
        public Builder summary(String summary) {
            result.setSummary(summary);
            return this;
        }
        public Builder meta(ObjectNode meta) {
            result.setMeta(meta);
            return this;
        }
        public DispatchExecutionResult build() {
            return result;
        }
    }
}
```

[DispatchExecutionResult.java](../../../../lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/result/DispatchExecutionResult.java)

---

```java
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
        return new ResultContent(TYPE_TEXT, MimeTypeUtils.TEXT_PLAIN, DEFAULT_OBJECT_MAPPER.valueToTree(value));
    }
    public static ResultContent text(JsonNode value) {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.isTextual()) {throw new IllegalArgumentException("text content requires a textual JsonNode");}
        return new ResultContent(TYPE_TEXT, MimeTypeUtils.TEXT_PLAIN, value);
    }
    public static ResultContent object(JsonNode value) {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.isObject()) {throw new IllegalArgumentException("object content requires an object JsonNode");}
        return json(value);
    }
    public static ResultContent array(JsonNode value) {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.isArray()) {throw new IllegalArgumentException("array content requires an array JsonNode");}
        return json(value);
    }
    public static ResultContent number(Number value) {
        Objects.requireNonNull(value, "value must not be null");
        return json(DEFAULT_OBJECT_MAPPER.valueToTree(value));
    }
    public static ResultContent bool(boolean value) {return json(DEFAULT_OBJECT_MAPPER.valueToTree(value));}
    public static ResultContent json(JsonNode value) {return mime(MimeTypeUtils.APPLICATION_JSON, value);}
    public static ResultContent mime(MimeType mimeType, JsonNode value) {
        Objects.requireNonNull(mimeType, "mimeType must not be null");
        Objects.requireNonNull(value, "value must not be null");
        String type = MimeTypeUtils.APPLICATION_JSON.includes(mimeType) ? TYPE_JSON : TYPE_MIME;
        return new ResultContent(type, mimeType, value);
    }
    public String type() {return type;}
    public MimeType mimeType() {return mimeType;}
    public JsonNode value() {return value;}
    public ObjectNode toJson() {return toJson(DEFAULT_OBJECT_MAPPER);}
    public ObjectNode toJson(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        if (TYPE_TEXT.equals(type)) {
            node.set("text", value);
            return node;
        }
        if (mimeType != null) {node.put("mimeType", mimeType.toString());}
        node.set("data", value);
        return node;
    }
}
```

[ResultContent.java](../../../../lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/result/ResultContent.java)
