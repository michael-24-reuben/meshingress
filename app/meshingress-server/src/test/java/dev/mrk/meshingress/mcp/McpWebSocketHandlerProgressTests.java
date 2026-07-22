package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcResponses;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpWebSocketHandlerProgressTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emitsCorrelatedProgressNotificationsForWebSocketToolCalls() throws Exception {
        McpTransportDispatcher transportDispatcher = mock(McpTransportDispatcher.class);
        when(transportDispatcher.dispatch(anyString(), any(McpInvocationFactory.class))).thenAnswer(invocation -> {
            McpInvocationFactory factory = invocation.getArgument(1);
            McpInvocation request = factory.create(objectMapper.readTree((String) invocation.getArgument(0)));
            request.context().progressReporter().plan(Duration.ofSeconds(45), 3, java.util.List.of("downloading"), "Starting");
            request.context().progressReporter().update("downloading", 1, 3, "Downloaded chapter 1");
            return Optional.empty();
        });

        McpWebSocketHandler handler = new McpWebSocketHandler(
                new MeshingressProperties(null, null, null, null, null, null, null, null, null, null, null),
                objectMapper,
                transportDispatcher,
                mock(JsonRpcResponses.class)
        );
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of(
                "mcp.sessionId", "session-1",
                "mcp.requestId", "header-request-1"
        ));

        handler.handleTextMessage(session, new TextMessage("""
                {"jsonrpc":"2.0","id":42,"method":"tools/call","params":{"name":"toonverse.download-book","arguments":{}}}
                """));

        var messages = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.times(2)).sendMessage(messages.capture());

        JsonNode planned = objectMapper.readTree(messages.getAllValues().getFirst().getPayload());
        assertEquals("notifications/progress", planned.path("method").asString());
        assertEquals(42, planned.path("params").path("callId").asInt());
        assertEquals("session-1", planned.path("params").path("sessionId").asString());
        assertEquals("PLANNED", planned.path("params").path("progress").path("state").asString());
        assertEquals("PT45S", planned.path("params").path("progress").path("estimate").asString());

        JsonNode update = objectMapper.readTree(messages.getAllValues().getLast().getPayload());
        assertEquals("IN_PROGRESS", update.path("params").path("progress").path("state").asString());
        assertEquals(1, update.path("params").path("progress").path("unitsCompleted").asInt());
    }

    @Test
    void emitsSeparateObservedEstimateNotifications() throws Exception {
        McpTransportDispatcher transportDispatcher = mock(McpTransportDispatcher.class);
        when(transportDispatcher.dispatch(anyString(), any(McpInvocationFactory.class))).thenAnswer(invocation -> {
            McpInvocationFactory factory = invocation.getArgument(1);
            McpInvocation request = factory.create(objectMapper.readTree((String) invocation.getArgument(0)));
            request.context().progressReporter().plan(Duration.ofMinutes(10), 10, java.util.List.of("downloading"), "Starting");
            request.context().progressReporter().update("downloading", 1, 10, "Downloaded chapter 1");
            Thread.sleep(20);
            request.context().progressReporter().update("downloading", 2, 10, "Downloaded chapter 2");
            request.context().progressReporter().complete("Done");
            return Optional.empty();
        });

        McpWebSocketHandler handler = new McpWebSocketHandler(
                new MeshingressProperties(null, null, null, null, null, null, null, null, null, null, null),
                objectMapper,
                transportDispatcher,
                mock(JsonRpcResponses.class)
        );
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of("mcp.sessionId", "session-1"));

        handler.handleTextMessage(session, new TextMessage("""
                {"jsonrpc":"2.0","id":"estimate-call","method":"tools/call","params":{"name":"toonverse.download-book","arguments":{}}}
                """));

        var messages = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.timeout(1_000).times(5)).sendMessage(messages.capture());
        JsonNode estimate = messages.getAllValues().stream()
                .map(TextMessage::getPayload)
                .map(payload -> {
                    try {
                        return objectMapper.readTree(payload);
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                })
                .filter(message -> "notifications/progress/estimate".equals(message.path("method").asString()))
                .findFirst()
                .orElseThrow();
        assertEquals("estimate-call", estimate.path("params").path("callId").asString());
        assertEquals(2, estimate.path("params").path("estimate").path("unitsCompleted").asInt());
        assertEquals(10, estimate.path("params").path("estimate").path("total").asInt());
        assertTrue(estimate.path("params").path("estimate").path("estimatedRemaining").asText().startsWith("PT"));
    }
}
