package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcResponses;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;

@Component
public class McpWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpWebSocketHandler.class);
    private final MeshingressProperties properties;

    private final ObjectMapper objectMapper;
    private final McpTransportDispatcher transportDispatcher;
    private final JsonRpcResponses responses;

    public McpWebSocketHandler(MeshingressProperties properties, ObjectMapper objectMapper, McpTransportDispatcher transportDispatcher, JsonRpcResponses responses) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transportDispatcher = transportDispatcher;
        this.responses = responses;
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        MeshingressProperties.Mcp.WebSocket websocket = properties.mcp().websocket();
        String requestId = stringAttribute(session.getAttributes(), "mcp.requestId");
        String sessionId = stringAttribute(session.getAttributes(), "mcp.sessionId");

        long maxBytes = websocket.maxMessageSize().toBytes();
        if (message.getPayloadLength() > maxBytes) {
            LOGGER.warn(
                    "MCP ws message too large: requestId={} sessionId={} payloadLength={} maxBytes={}",
                    requestId,
                    sessionId,
                    message.getPayloadLength(),
                    maxBytes
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Message too large")
            )));
            return;
        }

        LOGGER.info("=== MCP REQUEST START [ws] requestId={} sessionId={} wsSession={} ===", requestId, sessionId, session.getId());
        McpCallContext context = contextFrom(session.getAttributes());
        Optional<JsonNode> response = transportDispatcher.dispatch(message.getPayload(), context);
        if (response.isPresent()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response.get())));
        }
    }

    private McpCallContext contextFrom(Map<String, Object> attributes) {
        return new McpCallContext(
                stringAttribute(attributes, "mcp.authorization"),
                stringAttribute(attributes, "mcp.role"),
                stringAttribute(attributes, "mcp.sessionId"),
                stringAttribute(attributes, "mcp.requestId")
        );
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof String text ? text : null;
    }
}
