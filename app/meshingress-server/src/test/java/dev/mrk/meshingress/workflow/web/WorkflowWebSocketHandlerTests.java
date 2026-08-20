package dev.mrk.meshingress.workflow.web;

import dev.mrk.meshingress.workflow.WorkflowRun;
import dev.mrk.meshingress.workflow.WorkflowRunListener;
import dev.mrk.meshingress.workflow.WorkflowRuntime;
import dev.mrk.meshingress.workflow.WorkflowDefinition;
import dev.mrk.meshingress.workflow.WorkflowInput;
import dev.mrk.meshingress.workflow.WorkflowNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowWebSocketHandlerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void streamsStartedNodeAndCompletedEventsForTheSubmittedStudioDefinition() throws Exception {
        WorkflowRuntime runtime = mock(WorkflowRuntime.class);
        WorkflowRun completedRun = new WorkflowRun(
                "run_live",
                WorkflowRun.Status.COMPLETED,
                Map.of("greeting", JsonNodeFactory.instance.objectNode().put("text", "Hello")),
                List.of(new WorkflowRun.NodeOutcome("r-002", "default", 1, false, null)),
                null
        );
        doAnswer(invocation -> {
            WorkflowRunListener listener = invocation.getArgument(3);
            listener.onRunStarted("run_live");
            listener.onNodeStarted("r-002");
            listener.onNodeCompleted(completedRun.nodeOutcomes().getFirst());
            listener.onRunCompleted(completedRun);
            return completedRun;
        }).when(runtime).run(any(), any(), any(), any());

        WorkflowWebSocketHandler handler = new WorkflowWebSocketHandler(runtime, objectMapper);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("workflow-ws-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of("mcp.sessionId", "studio-session"));

        handler.handleTextMessage(session, new TextMessage("""
                {
                  "action":"run",
                  "requestId":"studio-request",
                  "definition": {
                    "id":"studio-live",
                    "version":1,
                    "nodes":[
                      {"requestId":"r-001","kind":"trigger","arguments":{},"output":"trigger"},
                      {"requestId":"r-003","kind":"tool","functionName":"helloworld.greeting.greet","arguments":{"name":"From Studio input"},"output":"greeting"}
                    ],
                    "edges":[{"source":"r-001","target":"r-003"}]
                  }
                }
                """));

        var messages = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(4)).sendMessage(messages.capture());
        var definition = org.mockito.ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(runtime).run(definition.capture(), any(), any(), any());
        WorkflowNode.ToolCall greeting = (WorkflowNode.ToolCall) definition.getValue().nodes().get(1);
        assertEquals("From Studio input", ((WorkflowInput.Literal) greeting.arguments().get("name")).value().asString());

        JsonNode started = objectMapper.readTree(messages.getAllValues().get(0).getPayload());
        assertEquals("workflow.started", started.path("type").asString());
        assertEquals("studio-request", started.path("requestId").asString());
        assertEquals("run_live", started.path("runId").asString());

        JsonNode nodeStarted = objectMapper.readTree(messages.getAllValues().get(1).getPayload());
        assertEquals("workflow.node.started", nodeStarted.path("type").asString());
        assertEquals("r-002", nodeStarted.path("nodeRequestId").asString());

        JsonNode nodeCompleted = objectMapper.readTree(messages.getAllValues().get(2).getPayload());
        assertEquals("workflow.node.completed", nodeCompleted.path("type").asString());
        assertEquals("r-002", nodeCompleted.path("outcome").path("requestId").asString());

        JsonNode completed = objectMapper.readTree(messages.getAllValues().get(3).getPayload());
        assertEquals("workflow.completed", completed.path("type").asString());
        assertEquals("COMPLETED", completed.path("run").path("status").asString());
    }

    @Test
    void rejectsLegacySampleRequestsWithoutRunningAHardCodedDefinition() throws Exception {
        WorkflowRuntime runtime = mock(WorkflowRuntime.class);
        WorkflowWebSocketHandler handler = new WorkflowWebSocketHandler(runtime, objectMapper);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("workflow-ws-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of());

        handler.handleTextMessage(session, new TextMessage("""
                {"action":"run","requestId":"studio-request","sample":"desktop-volume-greeting-solo-leveling"}
                """));

        var message = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(message.capture());
        assertEquals("workflow.error", objectMapper.readTree(message.getValue().getPayload()).path("type").asString());
        verifyNoInteractions(runtime);
    }
}
