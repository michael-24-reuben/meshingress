package dev.mrk.meshingress.workflow;

/** Per-node retry and explicit error-route policy. */
public record WorkflowFailurePolicy(int maxAttempts, String errorPort, String errorResultName) {
    public WorkflowFailurePolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("max attempts must be at least one");
        }
        WorkflowDefinition.requireText(errorPort, "error port");
        WorkflowDefinition.requireText(errorResultName, "error result name");
    }

    public static WorkflowFailurePolicy failWorkflow() {
        return new WorkflowFailurePolicy(1, "error", "error");
    }
}
