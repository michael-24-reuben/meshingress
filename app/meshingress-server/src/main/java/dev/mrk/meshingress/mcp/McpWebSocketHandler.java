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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import dev.mrk.meshingress.security.McpTransportContextFactory;
import dev.mrk.meshingress.security.McpTransportEvidence;
import dev.mrk.meshingress.api.McpPrincipal;

import java.util.Map;
import java.util.Optional;

@Component
public class McpWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpWebSocketHandler.class);
    private final MeshingressProperties properties;

    private final ObjectMapper objectMapper;
    private final McpTransportDispatcher transportDispatcher;
    private final JsonRpcResponses responses;
    private final McpTransportContextFactory contextFactory;

    @Autowired
    public McpWebSocketHandler(MeshingressProperties properties, ObjectMapper objectMapper, McpTransportDispatcher transportDispatcher, JsonRpcResponses responses, McpTransportContextFactory contextFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transportDispatcher = transportDispatcher;
        this.responses = responses;
        this.contextFactory = contextFactory;
    }

    /** @deprecated Test-only compatibility constructor; production uses the injected context factory. */
    @Deprecated
    public McpWebSocketHandler(MeshingressProperties properties, ObjectMapper objectMapper, McpTransportDispatcher transportDispatcher, JsonRpcResponses responses) {
        this(properties, objectMapper, transportDispatcher, responses,
                new McpTransportContextFactory(ignored -> dev.mrk.meshingress.api.McpPrincipal.anonymous()));
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        MeshingressProperties.Mcp.WebSocket websocket = properties.mcp().websocket();
        McpTransportEvidence evidence = evidenceFrom(session.getAttributes(), session.getId());
        String requestId = evidence.correlationId();
        String sessionId = evidence.sessionId();

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
        Optional<JsonNode> response = transportDispatcher.dispatch(
                message.getPayload(),
                request -> websocketInvocation(session, contextFactory.create(evidence, principalFrom(session.getAttributes()), request), request)
        );
        if (response.isPresent()) {
            sendJson(session, response.get());
        }
    }

    private McpPrincipal principalFrom(Map<String, Object> attributes) {
        Object value = attributes.get("mcp.principal");
        return value instanceof McpPrincipal principal ? principal : McpPrincipal.anonymous();
    }

    private McpInvocation websocketInvocation(WebSocketSession session, McpCallContext context, JsonNode request) {
        JsonNode callId = request.get("id");
        McpProgressLifecycle lifecycle = new McpProgressLifecycle(
                progress -> sendProgress(session, context, callId, progress),
                estimate -> sendProgressEstimate(session, context, callId, estimate)
        );
        return McpInvocation.webSocket(context.withExecution(new dev.mrk.meshingress.api.McpExecutionControl(null, null, lifecycle.reporter())), lifecycle);
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

    private McpTransportEvidence evidenceFrom(Map<String, Object> attributes, String websocketSessionId) {
        Object value = attributes.get("mcp.transportEvidence");
        if (value instanceof McpTransportEvidence evidence) {
            return new McpTransportEvidence(evidence.authorization(), fallbackSessionId(evidence.sessionId(), websocketSessionId), evidence.correlationId(), evidence.transport());
        }
        return new McpTransportEvidence(
                stringAttribute(attributes, "mcp.authorization"),
                fallbackSessionId(stringAttribute(attributes, "mcp.sessionId"), websocketSessionId),
                stringAttribute(attributes, "mcp.requestId"),
                McpTransportEvidence.Transport.WEBSOCKET
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
