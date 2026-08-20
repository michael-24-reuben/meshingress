package dev.mrk.meshingress.workflow;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** Ephemeral beta-run result. Durable workflow instances are intentionally outside this slice. */
public record WorkflowRun(
        String runId,
        Status status,
        Map<String, JsonNode> results,
        List<NodeOutcome> nodeOutcomes,
        String failureMessage
) {
    public WorkflowRun {
        results = Map.copyOf(results);
        nodeOutcomes = List.copyOf(nodeOutcomes);
    }

    public enum Status {
        COMPLETED,
        FAILED
    }

    public record NodeOutcome(String requestId, String port, int attempts, boolean failed, String message) {
    }
}
