package dev.mrk.meshingress.mcp.jsonrpc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class JsonRpcResponses {

    private final ObjectMapper objectMapper;

    public JsonRpcResponses(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode success(JsonNode id, JsonNode result) {
        ObjectNode response = base(id);
        response.set("result", result == null ? objectMapper.createObjectNode() : result);
        return response;
    }

    public ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = base(id);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }

    public ObjectNode error(JsonNode id, int code, String message, JsonNode data) {
        ObjectNode response = error(id, code, message);
        if (data != null) {
            ((ObjectNode) response.get("error")).set("data", data);
        }
        return response;
    }

    private ObjectNode base(JsonNode id) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id == null ? objectMapper.nullNode() : id);
        return response;
    }
}
