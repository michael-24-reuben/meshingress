package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class DispatchExecutionResultJsonCodec {

    private final ObjectMapper objectMapper;

    public DispatchExecutionResultJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode toJson(DispatchExecutionResult result) {
        return result.toJson(objectMapper);
    }

    public DispatchExecutionResult fromJson(ObjectNode node) {
        DispatchExecutionResult.Builder builder = DispatchExecutionResult.builder();
        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode item : content) {
                ResultContent resultContent = contentFromJson(item);
                if (resultContent != null) {
                    builder.appendContent(resultContent);
                }
            }
        }
        JsonNode structuredContent = node.path("structuredContent");
        if (!structuredContent.isMissingNode() && !structuredContent.isNull()) {
            builder.structuredContent(structuredContent.deepCopy());
        }
        if (node.path("isError").asBoolean(false)) {
            builder.error(true);
        }
        JsonNode meta = node.path("_meta");
        if (meta.isObject()) {
            builder.meta(((ObjectNode) meta).deepCopy());
        }
        return builder.build();
    }

    private ResultContent contentFromJson(JsonNode item) {
        if (!item.isObject()) {
            return null;
        }
        String type = item.path("type").asString("");
        if (ResultContent.TYPE_TEXT.equals(type)) {
            JsonNode text = item.path("text");
            if (text.isString()) {
                return ResultContent.text(text);
            }
            return ResultContent.text(text.asString(""));
        }
        JsonNode data = item.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return null;
        }
        if (ResultContent.TYPE_JSON.equals(type)) {
            return ResultContent.json(data.deepCopy());
        }
        String mimeTypeValue = item.path("mimeType").asString(MimeTypeUtils.APPLICATION_JSON_VALUE);
        MimeType mimeType = MimeType.valueOf(mimeTypeValue);
        return ResultContent.mime(mimeType, data.deepCopy());
    }
}
