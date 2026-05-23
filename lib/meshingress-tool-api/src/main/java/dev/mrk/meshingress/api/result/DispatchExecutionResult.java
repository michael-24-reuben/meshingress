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
        this.structuredContent = Objects.requireNonNull(
                structuredContent,
                "structuredContent must not be null"
        );
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
        ObjectNode metaNode = meta == null
                ? objectMapper.createObjectNode()
                : meta.deepCopy();

        if (status != null && !status.isBlank()) {
            metaNode.put("status", status);
        }

        if (summary != null && !summary.isBlank()) {
            metaNode.put("summary", summary);
        }

        if (errorCode != null && !errorCode.isBlank()) {
            metaNode.put("errorCode", errorCode);
        }

        if (errorMessage != null && !errorMessage.isBlank()) {
            metaNode.put("errorMessage", errorMessage);
        }

        if (!metaNode.has("generatedAt")) {
            metaNode.put("generatedAt", Instant.now().toString());
        }

        return metaNode;
    }

    public static final class Builder {

        private final DispatchExecutionResult result = new DispatchExecutionResult();

        private Builder() {
        }

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