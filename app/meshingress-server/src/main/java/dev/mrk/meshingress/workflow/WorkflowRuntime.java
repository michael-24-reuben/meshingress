package dev.mrk.meshingress.workflow;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.mcp.tools.ToolExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Executes a compiled manual-trigger workflow in process. */
@Service
public class WorkflowRuntime {

    private final WorkflowCompiler compiler;
    private final ToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    public WorkflowRuntime(WorkflowCompiler compiler, ToolExecutor toolExecutor, ObjectMapper objectMapper) {
        this.compiler = compiler;
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
    }

    public WorkflowRun run(WorkflowDefinition definition, JsonNode triggerValue, McpCallContext parentContext) {
        return run(definition, triggerValue, parentContext, WorkflowRunListener.noop());
    }

    /**
     * Executes a workflow and emits lifecycle events to the supplied observer.
     * The observer is intended for transports such as the Studio WebSocket.
     */
    public WorkflowRun run(
            WorkflowDefinition definition,
            JsonNode triggerValue,
            McpCallContext parentContext,
            WorkflowRunListener listener
    ) {
        CompiledWorkflow compiled = compiler.compile(definition);
        String runId = "run_" + UUID.randomUUID();
        WorkflowRunListener effectiveListener = listener == null ? WorkflowRunListener.noop() : listener;
        Map<String, JsonNode> results = new LinkedHashMap<>();
        List<WorkflowRun.NodeOutcome> outcomes = new ArrayList<>();
        Map<String, Set<String>> arrivals = new HashMap<>();
        Set<String> completed = new HashSet<>();
        ArrayDeque<String> ready = new ArrayDeque<>();

        WorkflowNode.ManualTrigger trigger = compiled.nodesByRequestId().values().stream()
                .filter(WorkflowNode.ManualTrigger.class::isInstance)
                .map(WorkflowNode.ManualTrigger.class::cast)
                .findFirst()
                .orElseThrow();
        effectiveListener.onRunStarted(runId);
        ready.add(trigger.requestId());

        while (!ready.isEmpty()) {
            String requestId = ready.removeFirst();
            if (!completed.add(requestId)) {
                continue;
            }
            WorkflowNode node = compiled.nodesByRequestId().get(requestId);
            effectiveListener.onNodeStarted(requestId);
            NodeExecution execution = executeNode(node, triggerValue, results, parentContext, runId);
            WorkflowRun.NodeOutcome outcome = new WorkflowRun.NodeOutcome(requestId, execution.port(), execution.attempts(), execution.failed(), execution.message());
            outcomes.add(outcome);
            effectiveListener.onNodeCompleted(outcome);

            if (execution.failed() && compiled.outgoingByRequestId().get(requestId).stream()
                    .noneMatch(edge -> edge.from().port().equals(execution.port()))) {
                WorkflowRun failedRun = failed(runId, results, outcomes, execution.message());
                effectiveListener.onRunCompleted(failedRun);
                return failedRun;
            }
            if (!execution.failed()) {
                results.put(node.result().as(), execution.value());
            }
            for (WorkflowEdge edge : compiled.outgoingByRequestId().get(requestId)) {
                if (!edge.from().port().equals(execution.port())) {
                    continue;
                }
                String targetId = edge.to().requestId();
                WorkflowNode target = compiled.nodesByRequestId().get(targetId);
                if (target instanceof WorkflowNode.Join) {
                    arrivals.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(requestId);
                    if (arrivals.get(targetId).size() == compiled.incomingByRequestId().get(targetId).size()) {
                        ready.add(targetId);
                    }
                } else {
                    ready.add(targetId);
                }
            }
        }
        WorkflowRun completedRun = new WorkflowRun(runId, WorkflowRun.Status.COMPLETED, results, outcomes, null);
        effectiveListener.onRunCompleted(completedRun);
        return completedRun;
    }

    private WorkflowRun failed(
            String runId,
            Map<String, JsonNode> results,
            List<WorkflowRun.NodeOutcome> outcomes,
            String message
    ) {
        return new WorkflowRun(runId, WorkflowRun.Status.FAILED, results, outcomes, message);
    }

    private NodeExecution executeNode(
            WorkflowNode node,
            JsonNode triggerValue,
            Map<String, JsonNode> results,
            McpCallContext parentContext,
            String runId
    ) {
        WorkflowFailurePolicy failurePolicy = node.failurePolicy();
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= failurePolicy.maxAttempts(); attempt++) {
            try {
                JsonNode value = executeValue(node, triggerValue, results, parentContext, runId, attempt);
                validateResultType(node.result(), value, node.requestId());
                return new NodeExecution(value, node.result().routing().selectPort(value), attempt, false, null);
            } catch (Exception exception) {
                lastFailure = exception;
            }
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("requestId", node.requestId());
        error.put("message", lastFailure == null ? "Workflow node failed" : lastFailure.getMessage());
        error.put("attempts", failurePolicy.maxAttempts());
        results.put(failurePolicy.errorResultName(), error);
        return new NodeExecution(error, failurePolicy.errorPort(), failurePolicy.maxAttempts(), true, error.path("message").asString());
    }

    private JsonNode executeValue(
            WorkflowNode node,
            JsonNode triggerValue,
            Map<String, JsonNode> results,
            McpCallContext parentContext,
            String runId,
            int attempt
    ) {
        if (node instanceof WorkflowNode.ManualTrigger) {
            return triggerValue == null ? NullNode.getInstance() : triggerValue.deepCopy();
        }
        if (node instanceof WorkflowNode.ToolCall toolCall) {
            ObjectNode arguments = objectMapper.createObjectNode();
            toolCall.arguments().forEach((name, input) -> arguments.set(name, input.resolve(results)));
            McpCallContext context = (parentContext == null ? new McpCallContext(null, null, "", "") : parentContext)
                    .deriveWorkflowChild(runId, node.requestId(), attempt);
            DispatchExecutionResult result = toolExecutor.execute(toolCall.functionName(), arguments, context);
            if (result.isError()) {
                throw new WorkflowValidationException(toolFailureMessage(result));
            }
            return toolResultValue(result);
        }
        if (node instanceof WorkflowNode.Compose compose) {
            return resolveObject(compose.fields(), results);
        }
        if (node instanceof WorkflowNode.ExpressionTrue expression) {
            JsonNode value = expression.expression().resolve(results);
            if (!value.isBoolean()) {
                throw new WorkflowValidationException("expression.true input must resolve to a boolean");
            }
            return value;
        }
        if (node instanceof WorkflowNode.Join join) {
            return resolveObject(join.fields(), results);
        }
        if (node instanceof WorkflowNode.Merge) {
            return NullNode.getInstance();
        }
        throw new WorkflowValidationException("Unsupported workflow node: " + node.getClass().getSimpleName());
    }

    private ObjectNode resolveObject(Map<String, WorkflowInput> fields, Map<String, JsonNode> results) {
        ObjectNode value = objectMapper.createObjectNode();
        fields.forEach((name, input) -> value.set(name, input.resolve(results)));
        return value;
    }

    private JsonNode toolResultValue(DispatchExecutionResult result) {
        if (result.structuredContent().isPresent()) {
            return result.structuredContent().orElseThrow().deepCopy();
        }
        List<ResultContent> content = result.content();
        if (content.isEmpty()) {
            return NullNode.getInstance();
        }
        if (content.size() == 1) {
            return content.getFirst().value().deepCopy();
        }
        tools.jackson.databind.node.ArrayNode values = objectMapper.createArrayNode();
        content.forEach(item -> values.add(item.value()));
        return values;
    }

    private String toolFailureMessage(DispatchExecutionResult result) {
        JsonNode meta = result.toJson(objectMapper).path("_meta");
        String message = meta.path("errorMessage").asString("");
        if (!message.isBlank()) {
            return message;
        }
        return result.content().isEmpty() ? "Tool node returned an error" : result.content().getFirst().value().asString("Tool node returned an error");
    }

    private void validateResultType(WorkflowResultDeclaration declaration, JsonNode value, String requestId) {
        boolean matches = switch (declaration.type()) {
            case BOOLEAN -> value.isBoolean();
            case STRING -> value.isString();
            case NUMBER -> value.isNumber();
            case OBJECT -> value.isObject();
            case ARRAY -> value.isArray();
            case NULL -> value.isNull();
        };
        if (!matches) {
            throw new WorkflowValidationException("Result type mismatch for " + requestId + ": expected " + declaration.type());
        }
    }

    private record NodeExecution(JsonNode value, String port, int attempts, boolean failed, String message) {
    }
}
