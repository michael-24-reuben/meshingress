package dev.mrk.meshingress.controller.internal;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@McpDispatchMapping("")
public class InternalMcpController {

    private static final String PROTOCOL_VERSION = "2025-11-25";
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalMcpController.class);

    private final ObjectMapper objectMapper;

    public InternalMcpController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @McpDispatchMethod("initialize")
    public ObjectNode initialize(@McpDispatchParam("params") JsonNode params) {
        LOGGER.debug("MCP initialize requested");
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

    @McpDispatchMethod("notifications/initialized")
    public ObjectNode initialized(McpCallContext context) {
        LOGGER.debug("MCP initialized notification: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return objectMapper.createObjectNode();
    }

    @McpDispatchMethod("ping")
    public ObjectNode ping(McpCallContext context) {
        LOGGER.debug("MCP ping: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return objectMapper.createObjectNode();
    }
}
