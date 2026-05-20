package dev.mrk.meshingress.controller.internal;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.controller.McpMethodController;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Set;

@Component
public class InternalMcpController implements McpMethodController {

    private static final String PROTOCOL_VERSION = "2025-11-25";
    private static final Set<String> METHODS = Set.of(
            "initialize",
            "notifications/initialized",
            "ping"
    );

    private final ObjectMapper objectMapper;

    public InternalMcpController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<String> supportedMethods() {
        return METHODS;
    }

    @Override
    public boolean supports(String method) {
        return METHODS.contains(method);
    }

    @Override
    public JsonNode dispatch(String method, JsonNode params, McpCallContext context) {
        return switch (method) {
            case "initialize" -> initialize(params);
            case "notifications/initialized", "ping" -> objectMapper.createObjectNode();
            default -> throw new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found");
        };
    }

    private ObjectNode initialize(JsonNode params) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", params.path("protocolVersion").asString(PROTOCOL_VERSION));

        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode tools = objectMapper.createObjectNode();
        tools.put("listChanged", true);
        capabilities.set("tools", tools);
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "meshingress");
        serverInfo.put("version", "0.1.0");
        result.set("serverInfo", serverInfo);
        return result;
    }
}
