package dev.mrk.meshingress.workflow;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.dispatch.StructuredContentSchemaGenerator;
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
        List<WorkflowRun.NodeResult> nodeResults = new ArrayList<>();
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
            long startedAt = System.currentTimeMillis();
            effectiveListener.onNodeStarted(new WorkflowRun.NodeStarted(requestId, startedAt));
            NodeExecution execution = executeNode(node, triggerValue, results, parentContext, runId);
            long completedAt = Math.max(startedAt, System.currentTimeMillis());
            WorkflowRun.NodeOutcome outcome = new WorkflowRun.NodeOutcome(requestId, execution.port(), execution.attempts(), execution.failed(), execution.message());
            if (!execution.failed()) {
                results.put(node.result().as(), execution.value());
            }
            WorkflowRun.NodeResult nodeResult = new WorkflowRun.NodeResult(
                    requestId,
                    nodePath(node),
                    node.result().as(),
                    outcome,
                    startedAt,
                    completedAt,
                    execution.transportResult(),
                    execution.outputSchema(),
                    execution.diagnostics()
            );
            nodeResults.add(nodeResult);
            effectiveListener.onNodeCompleted(nodeResult);

            if (execution.failed() && compiled.outgoingByRequestId().get(requestId).stream()
                    .noneMatch(edge -> edge.from().port().equals(execution.port()))) {
                WorkflowRun failedRun = failed(runId, results, nodeResults, execution.message());
                effectiveListener.onRunCompleted(failedRun);
                return failedRun;
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
        WorkflowRun completedRun = new WorkflowRun(runId, WorkflowRun.Status.COMPLETED, results, nodeResults, null);
        effectiveListener.onRunCompleted(completedRun);
        return completedRun;
    }

    private WorkflowRun failed(
            String runId,
            Map<String, JsonNode> results,
            List<WorkflowRun.NodeResult> nodeResults,
            String message
    ) {
        return new WorkflowRun(runId, WorkflowRun.Status.FAILED, results, nodeResults, message);
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
                NodeValue value = executeValue(node, triggerValue, results, parentContext, runId, attempt);
                validateResultType(node.result(), value.value(), node.requestId());
                return new NodeExecution(
                        value.value(),
                        value.transportResult(),
                        value.outputSchema(),
                        value.diagnostics(),
                        node.result().routing().selectPort(value.value()),
                        attempt,
                        false,
                        null
                );
            } catch (Exception exception) {
                lastFailure = exception;
            }
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("requestId", node.requestId());
        error.put("message", lastFailure == null ? "Workflow node failed" : lastFailure.getMessage());
        error.put("attempts", failurePolicy.maxAttempts());
        results.put(failurePolicy.errorResultName(), error);
        return new NodeExecution(error, error, null, List.of(), failurePolicy.errorPort(), failurePolicy.maxAttempts(), true, error.path("message").asString());
    }

    private NodeValue executeValue(
            WorkflowNode node,
            JsonNode triggerValue,
            Map<String, JsonNode> results,
            McpCallContext parentContext,
            String runId,
            int attempt
    ) {
        if (node instanceof WorkflowNode.ManualTrigger) {
            JsonNode value = triggerValue == null ? NullNode.getInstance() : triggerValue.deepCopy();
            return NodeValue.of(value);
        }
        if (node instanceof WorkflowNode.ToolCall toolCall) {
            ObjectNode arguments = objectMapper.createObjectNode();
            toolCall.arguments().forEach((name, input) -> arguments.set(name, input.resolve(results)));
            McpCallContext context = (parentContext == null ? new McpCallContext(null, null, "", runId) : parentContext)
                    .deriveWorkflowChild(runId, node.requestId(), attempt);
            DispatchExecutionResult result = toolExecutor.execute(toolCall.functionName(), arguments, context);
            if (result.isError()) {
                throw new WorkflowValidationException(toolFailureMessage(result));
            }
            return toolResultValue(result);
        }
        if (node instanceof WorkflowNode.Compose compose) {
            return NodeValue.of(resolveObject(compose.fields(), results));
        }
        if (node instanceof WorkflowNode.ExpressionTrue expression) {
            JsonNode value = expression.expression().resolve(results);
            if (!value.isBoolean()) {
                throw new WorkflowValidationException("expression.true input must resolve to a boolean");
            }
            return NodeValue.of(value);
        }
        if (node instanceof WorkflowNode.Join join) {
            return NodeValue.of(resolveObject(join.fields(), results));
        }
        if (node instanceof WorkflowNode.Merge) {
            return NodeValue.of(NullNode.getInstance());
        }
        throw new WorkflowValidationException("Unsupported workflow node: " + node.getClass().getSimpleName());
    }

    private ObjectNode resolveObject(Map<String, WorkflowInput> fields, Map<String, JsonNode> results) {
        ObjectNode value = objectMapper.createObjectNode();
        fields.forEach((name, input) -> value.set(name, input.resolve(results)));
        return value;
    }

    private NodeValue toolResultValue(DispatchExecutionResult result) {
        JsonNode serialized = result.toJson(objectMapper);
        JsonNode structuredContent = serialized.get("structuredContent");
        if (structuredContent != null && !structuredContent.isNull()) {
            JsonNode outputSchema = result.typedStructuredContent()
                    .flatMap(content -> StructuredContentSchemaGenerator.outputSchemaFor(objectMapper, content))
                    .orElse(null);
            return new NodeValue(
                    structuredContentData(structuredContent),
                    structuredContent.deepCopy(),
                    outputSchema,
                    diagnostics(serialized)
            );
        }
        List<ResultContent> content = result.content();
        if (content.isEmpty()) {
            return NodeValue.of(NullNode.getInstance());
        }
        if (content.size() == 1) {
            return NodeValue.of(content.getFirst().value().deepCopy());
        }
        tools.jackson.databind.node.ArrayNode values = objectMapper.createArrayNode();
        content.forEach(item -> values.add(item.value()));
        return NodeValue.of(values);
    }

    private JsonNode structuredContentData(JsonNode structuredContent) {
        if (structuredContent.isObject()
                && structuredContent.path("kind").isTextual()
                && structuredContent.path("schema").isTextual()
                && structuredContent.path("version").isIntegralNumber()
                && structuredContent.has("data")) {
            return structuredContent.get("data").deepCopy();
        }
        return structuredContent.deepCopy();
    }

    private String nodePath(WorkflowNode node) {
        return node instanceof WorkflowNode.ToolCall toolCall ? toolCall.functionName() : null;
    }

    private String toolFailureMessage(DispatchExecutionResult result) {
        JsonNode meta = result.toJson(objectMapper).path("_meta");
        String message = meta.path("errorMessage").asString("");
        if (!message.isBlank()) {
            return message;
        }
        return result.content().isEmpty() ? "Tool node returned an error" : result.content().getFirst().value().asString("Tool node returned an error");
    }

    private List<WorkflowRun.Diagnostic> diagnostics(JsonNode serialized) {
        JsonNode validation = serialized.at("/_meta/meshingress/outputSchema");
        if (!validation.isObject()) {
            return List.of();
        }
        if (validation.path("validated").asBoolean(false) && validation.path("valid").asBoolean(true)) {
            return List.of();
        }
        String type = validation.path("validated").asBoolean(false)
                ? "output.schema.violation"
                : "output.schema.unavailable";
        String message = validation.path("reason").asText("");
        if (message.isBlank()) {
            message = type.equals("output.schema.violation")
                    ? "Structured content does not match the declared output schema."
                    : "The declared output schema could not be evaluated.";
        }
        return List.of(new WorkflowRun.Diagnostic(type, "warning", message, validation));
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

    private record NodeValue(JsonNode value, JsonNode transportResult, JsonNode outputSchema, List<WorkflowRun.Diagnostic> diagnostics) {
        private static NodeValue of(JsonNode value) {
            return new NodeValue(value, value, null, List.of());
        }
    }

    private record NodeExecution(
            JsonNode value,
            JsonNode transportResult,
            JsonNode outputSchema,
            List<WorkflowRun.Diagnostic> diagnostics,
            String port,
            int attempts,
            boolean failed,
            String message
    ) {
    }
}
