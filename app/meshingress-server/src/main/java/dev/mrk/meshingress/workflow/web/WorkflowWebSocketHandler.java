package dev.mrk.meshingress.workflow.web;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.security.McpTransportContextFactory;
import dev.mrk.meshingress.security.McpTransportEvidence;
import dev.mrk.meshingress.workflow.WorkflowRun;
import dev.mrk.meshingress.workflow.WorkflowRunListener;
import dev.mrk.meshingress.workflow.WorkflowRuntime;
import dev.mrk.meshingress.workflow.WorkflowDefinition;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.UUID;

/** Streams lifecycle events for submitted Studio workflow definitions. */
@Component
public class WorkflowWebSocketHandler extends TextWebSocketHandler {

    public static final String PATH = "/api/v1/workflows/ws";
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowWebSocketHandler.class);

    private final WorkflowRuntime workflowRuntime;
    private final ObjectMapper objectMapper;
    private final McpTransportContextFactory contextFactory;

    @Autowired
    public WorkflowWebSocketHandler(WorkflowRuntime workflowRuntime, ObjectMapper objectMapper, McpTransportContextFactory contextFactory) {
        this.workflowRuntime = workflowRuntime;
        this.objectMapper = objectMapper;
        this.contextFactory = contextFactory;
    }

    /** @deprecated Test-only compatibility constructor; production uses the injected context factory. */
    @Deprecated
    public WorkflowWebSocketHandler(WorkflowRuntime workflowRuntime, ObjectMapper objectMapper) {
        this(workflowRuntime, objectMapper, new McpTransportContextFactory(ignored -> dev.mrk.meshingress.api.McpPrincipal.anonymous()));
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        JsonNode request;
        try {
            request = objectMapper.readTree(message.getPayload());
        } catch (JacksonException exception) {
            sendError(session, null, "Workflow request must be valid JSON.");
            return;
        }
        if (!"run".equals(request.path("action").asString())) {
            sendError(session, requestId(request), "Unsupported workflow action.");
            return;
        }
        String requestId = requestId(request);
        McpCallContext context = contextFrom(session.getAttributes(), session.getId(), requestId);
        LOGGER.info("Workflow WebSocket run requested: requestId={} sessionId={}", context.requestId(), context.sessionId());
        try {
            WorkflowDefinition definition = definitionFrom(request);
            workflowRuntime.run(
                    definition,
                    null,
                    context,
                    new WorkflowWebSocketListener(session, requestId)
            );
        } catch (Exception exception) {
            LOGGER.warn("Workflow WebSocket run failed: requestId={} sessionId={}", context.requestId(), context.sessionId(), exception);
            sendError(session, requestId, exception.getMessage() == null ? "Workflow run failed." : exception.getMessage());
        }
    }

    private WorkflowDefinition definitionFrom(JsonNode request) throws JacksonException {
        JsonNode submittedDefinition = request.get("definition");
        if (submittedDefinition != null && !submittedDefinition.isNull()) {
            StudioWorkflowDefinition definition = objectMapper.readValue(
                    objectMapper.writeValueAsString(submittedDefinition),
                    StudioWorkflowDefinition.class
            );
            return definition.executableDefinition();
        }
        throw new IllegalArgumentException("A Studio workflow definition is required.");
    }

    private String requestId(JsonNode request) {
        String provided = request.path("requestId").asString();
        return provided.isBlank() ? UUID.randomUUID().toString() : provided;
    }

    private McpCallContext contextFrom(Map<String, Object> attributes, String webSocketSessionId, String requestId) {
        Object value = attributes.get("mcp.transportEvidence");
        McpTransportEvidence evidence = value instanceof McpTransportEvidence transportEvidence
                ? transportEvidence
                : new McpTransportEvidence(null, webSocketSessionId, requestId, McpTransportEvidence.Transport.WORKFLOW);
        String sessionId = evidence.sessionId() == null || evidence.sessionId().isBlank() ? webSocketSessionId : evidence.sessionId();
        return contextFactory.create(new McpTransportEvidence(evidence.authorization(), sessionId, requestId, McpTransportEvidence.Transport.WORKFLOW), null);
    }

    private void sendError(WebSocketSession session, String requestId, String message) throws Exception {
        ObjectNode event = event("workflow.error", requestId);
        event.put("message", message);
        send(session, event);
    }

    private ObjectNode event(String type, String requestId) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", type);
        if (requestId != null && !requestId.isBlank()) {
            event.put("requestId", requestId);
        }
        return event;
    }

    private void send(WebSocketSession session, JsonNode payload) throws Exception {
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        }
    }

    private final class WorkflowWebSocketListener implements WorkflowRunListener {
        private final WebSocketSession session;
        private final String requestId;

        private WorkflowWebSocketListener(WebSocketSession session, String requestId) {
            this.session = session;
            this.requestId = requestId;
        }

        @Override
        public void onRunStarted(String runId) {
            ObjectNode event = sequencedEvent("workflow.started");
            event.put("runId", runId);
            safelySend(event);
        }

        @Override
        public void onNodeStarted(WorkflowRun.NodeStarted nodeStarted) {
            ObjectNode event = sequencedEvent("workflow.node.started");
            event.put("nodeRequestId", nodeStarted.nodeId());
            event.put("startedAt", nodeStarted.startedAt());
            safelySend(event);
        }

        @Override
        public void onNodeCompleted(WorkflowRun.NodeResult nodeResult) {
            ObjectNode event = sequencedEvent("workflow.node.completed");
            event.set("node", objectMapper.valueToTree(nodeResult));
            event.put("completedAt", nodeResult.completedAt());
            safelySend(event);
        }

        @Override
        public void onRunCompleted(WorkflowRun run) {
            ObjectNode event = sequencedEvent("workflow.completed");
            event.set("run", objectMapper.valueToTree(run));
            safelySend(event);
        }

        private void safelySend(JsonNode event) {
            try {
                send(session, event);
            } catch (Exception exception) {
                LOGGER.warn("Unable to send workflow WebSocket event: requestId={}", requestId, exception);
            }
        }

        private long nextSequence = 1;

        private ObjectNode sequencedEvent(String type) {
            ObjectNode event = event(type, requestId);
            event.put("sequence", nextSequence++);
            return event;
        }
    }
}
