package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.controller.McpDispatcher;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;

@Component
public class McpWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final McpDispatcher dispatcher;
    private final JsonRpcResponses responses;

    public McpWebSocketHandler(ObjectMapper objectMapper, McpDispatcher dispatcher, JsonRpcResponses responses) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.responses = responses;
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        if (!session.getAttributes().containsKey(McpAuthHandshakeInterceptor.AUTHENTICATED_SESSION_ATTRIBUTE)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthenticated MCP session"));
            return;
        }

        JsonNode request;
        try {
            request = objectMapper.readTree(message.getPayload());
        } catch (JacksonException exception) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    responses.error(null, JsonRpcErrorCodes.PARSE_ERROR, "Parse error")
            )));
            return;
        }

        McpCallContext context = contextFrom(session.getAttributes());
        Optional<JsonNode> response = dispatcher.dispatch(request, context);
        if (response.isPresent()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response.get())));
        }
    }

    private McpCallContext contextFrom(Map<String, Object> attributes) {
        return new McpCallContext(
                stringAttribute(attributes, McpAuthHandshakeInterceptor.AUTHORIZATION_ATTRIBUTE),
                stringAttribute(attributes, McpAuthHandshakeInterceptor.ROLE_ATTRIBUTE),
                stringAttribute(attributes, McpAuthHandshakeInterceptor.SESSION_ID_ATTRIBUTE),
                stringAttribute(attributes, McpAuthHandshakeInterceptor.REQUEST_ID_ATTRIBUTE)
        );
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof String text ? text : null;
    }
}
