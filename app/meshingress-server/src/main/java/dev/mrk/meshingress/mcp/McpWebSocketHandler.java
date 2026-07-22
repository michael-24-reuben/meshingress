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
import tools.jackson.databind.node.ObjectNode;
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

            sendJson(session, responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Message too large"));
            return;
        }

        LOGGER.info("=== MCP REQUEST START [ws] requestId={} sessionId={} wsSession={} ===", requestId, sessionId, session.getId());
        McpCallContext context = contextFrom(session.getAttributes(), session.getId());
        Optional<JsonNode> response = transportDispatcher.dispatch(
                message.getPayload(),
                request -> websocketInvocation(session, context, request)
        );
        if (response.isPresent()) {
            sendJson(session, response.get());
        }
    }

    private McpInvocation websocketInvocation(WebSocketSession session, McpCallContext context, JsonNode request) {
        JsonNode callId = request.get("id");
        McpProgressLifecycle lifecycle = new McpProgressLifecycle(
                progress -> sendProgress(session, context, callId, progress),
                estimate -> sendProgressEstimate(session, context, callId, estimate)
        );
        return McpInvocation.webSocket(context.withProgressReporter(lifecycle.reporter()), lifecycle);
    }

    private void sendProgress(WebSocketSession session, McpCallContext context, JsonNode callId, dev.mrk.meshingress.api.result.progress.ProgressUpdate progress) {
        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", "notifications/progress");
        ObjectNode params = notification.putObject("params");
        if (callId != null && !callId.isNull()) {
            params.set("callId", callId.deepCopy());
        }
        params.put("sessionId", context.sessionId());
        if (context.requestId() != null && !context.requestId().isBlank()) {
            params.put("requestId", context.requestId());
        }
        ObjectNode event = params.putObject("progress");
        event.put("state", progress.state().name());
        event.put("phase", progress.phase());
        event.put("unitsCompleted", progress.unitsCompleted());
        event.put("total", progress.total());
        event.put("message", progress.message());
        if (progress.estimate() != null) {
            event.put("estimate", progress.estimate().toString());
        }
        var phases = event.putArray("phases");
        progress.phases().forEach(phases::add);
        try {
            sendJson(session, notification);
        } catch (Exception exception) {
            LOGGER.warn("Unable to send MCP progress notification: sessionId={} requestId={}", context.sessionId(), context.requestId(), exception);
        }
    }

    private void sendProgressEstimate(WebSocketSession session, McpCallContext context, JsonNode callId, McpProgressEstimate estimate) {
        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", "notifications/progress/estimate");
        ObjectNode params = notification.putObject("params");
        if (callId != null && !callId.isNull()) {
            params.set("callId", callId.deepCopy());
        }
        params.put("sessionId", context.sessionId());
        if (context.requestId() != null && !context.requestId().isBlank()) {
            params.put("requestId", context.requestId());
        }
        ObjectNode event = params.putObject("estimate");
        event.put("unitsCompleted", estimate.unitsCompleted());
        event.put("total", estimate.total());
        event.put("sampleUnits", estimate.sampleUnits());
        event.put("elapsed", estimate.elapsed().toString());
        event.put("sampleDuration", estimate.sampleDuration().toString());
        event.put("observedSecondsPerUnit", estimate.observedSecondsPerUnit());
        event.put("estimatedRemaining", estimate.estimatedRemaining().toString());
        event.put("estimatedTotal", estimate.estimatedTotal().toString());
        try {
            sendJson(session, notification);
        } catch (Exception exception) {
            LOGGER.warn("Unable to send MCP progress estimate: sessionId={} requestId={}", context.sessionId(), context.requestId(), exception);
        }
    }

    private void sendJson(WebSocketSession session, JsonNode payload) throws Exception {
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        }
    }

    private McpCallContext contextFrom(Map<String, Object> attributes, String websocketSessionId) {
        return new McpCallContext(
                stringAttribute(attributes, "mcp.authorization"),
                stringAttribute(attributes, "mcp.role"),
                fallbackSessionId(stringAttribute(attributes, "mcp.sessionId"), websocketSessionId),
                stringAttribute(attributes, "mcp.requestId")
        );
    }

    private String fallbackSessionId(String sessionId, String websocketSessionId) {
        return sessionId == null || sessionId.isBlank() ? websocketSessionId : sessionId;
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof String text ? text : null;
    }
}
