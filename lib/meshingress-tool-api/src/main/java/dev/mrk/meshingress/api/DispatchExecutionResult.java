package dev.mrk.meshingress.api;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class DispatchExecutionResult {
    protected final JsonNode content;
    protected final JsonNode structuredContent;
    protected final boolean error;

    public DispatchExecutionResult(ArrayNode content, JsonNode structuredContent, boolean error) {
        this.content = content;
        this.structuredContent = structuredContent;
        this.error = error;
    }

    @Contract("_, _, _ -> new")
    public static @NonNull DispatchExecutionResult text(@NonNull ObjectMapper objectMapper, String text, JsonNode structuredContent) {
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", text);
        content.add(textContent);
        return new DispatchExecutionResult(content, structuredContent, false);
    }

    @Contract("_, _ -> new")
    public static @NonNull DispatchExecutionResult error(@NonNull ObjectMapper objectMapper, String text) {
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", text);
        content.add(textContent);
        return new DispatchExecutionResult(content, null, true);
    }

    public ObjectNode toJson(@NonNull ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set("content", content == null ? objectMapper.createArrayNode() : content);
        if (structuredContent != null && !structuredContent.isNull()) {
            result.set("structuredContent", structuredContent);
        }
        result.put("isError", error);
        return result;
    }
}
