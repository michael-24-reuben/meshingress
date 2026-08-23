package dev.mrk.meshingress.workflow;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** Ephemeral beta-run result. Durable workflow instances are intentionally outside this slice. */
public record WorkflowRun(
        String runId,
        Status status,
        Map<String, JsonNode> results,
        List<NodeResult> nodeResults,
        String failureMessage
) {
    public WorkflowRun {
        results = Map.copyOf(results);
        nodeResults = List.copyOf(nodeResults);
    }

    public enum Status {
        COMPLETED,
        FAILED
    }

    public record NodeOutcome(String requestId, String port, int attempts, boolean failed, String message) {
    }

    /** Server-authoritative start boundary for one node execution. */
    public record NodeStarted(String nodeId, long startedAt) {
    }

    /**
     * Transport-facing execution record. {@code result} retains the tool's structured-content envelope,
     * while {@link WorkflowRun#results()} contains only payload values for workflow dataflow.
     */
    public record NodeResult(
            String nodeId,
            String nodePath,
            String variable,
            NodeOutcome outcome,
            long startedAt,
            long completedAt,
            JsonNode result,
            JsonNode outputSchema,
            List<Diagnostic> diagnostics
    ) {
        public NodeResult {
            result = result == null ? null : result.deepCopy();
            outputSchema = outputSchema == null ? null : outputSchema.deepCopy();
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }

    /** A non-fatal, machine-readable observation made while executing a node. */
    public record Diagnostic(String type, String severity, String message, JsonNode details) {
        public Diagnostic {
            details = details == null ? null : details.deepCopy();
        }
    }
}
