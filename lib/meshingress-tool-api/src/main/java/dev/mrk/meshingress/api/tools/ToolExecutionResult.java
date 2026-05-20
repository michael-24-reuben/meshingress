package dev.mrk.meshingress.api.tools;

import dev.mrk.meshingress.api.DispatchExecutionResult;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class ToolExecutionResult extends DispatchExecutionResult {
    private static final ObjectMapper mapper = new ObjectMapper();
    public ToolExecutionResult(ArrayNode content, JsonNode structuredContent, boolean error) {
        super(content, structuredContent, error);
    }

    @Contract("_, _, _ -> new")
    public static @NonNull ToolExecutionResult text(@NonNull ObjectMapper objectMapper, String text, JsonNode structuredContent) {
        ArrayNode content = textContent(objectMapper, text);
        return new ToolExecutionResult(content, structuredContent, false);
    }

    @Contract("_, _ -> new")
    public static @NonNull ToolExecutionResult text(String text, JsonNode structuredContent) {
        ArrayNode content = textContent(mapper, text);
        return new ToolExecutionResult(content, structuredContent, false);
    }

    @Contract("_, _ -> new")
    public static @NonNull ToolExecutionResult error(@NonNull ObjectMapper objectMapper, String text) {
        ArrayNode content = textContent(objectMapper, text);
        return new ToolExecutionResult(content, null, true);
    }

    private static ArrayNode textContent(ObjectMapper objectMapper, String text) {
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", text);
        content.add(textContent);
        return content;
    }

}
