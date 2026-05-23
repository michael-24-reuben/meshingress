package dev.mrk.meshingress.route.framework.dispatch;

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
