package dev.mrk.meshingress.dispatch.invoker;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class McpReturnValueAdapter {

    public JsonNode toJsonNode(Object value, ObjectMapper objectMapper) {
        if (value == null) {
            return objectMapper.createObjectNode();
        }
        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        return objectMapper.valueToTree(value);
    }
}
