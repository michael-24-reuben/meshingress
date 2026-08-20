package dev.mrk.meshingress.api;

/** Parent-call relationship for internal workflow executions. */
public record McpCallLineage(String parentInvocationId, String workflowRunId, String workflowNodeId, Integer workflowAttempt) {
    public McpCallLineage {
        parentInvocationId = text(parentInvocationId);
        workflowRunId = text(workflowRunId);
        workflowNodeId = text(workflowNodeId);
        workflowAttempt = workflowAttempt != null && workflowAttempt > 0 ? workflowAttempt : null;
    }
    public static McpCallLineage root() { return new McpCallLineage("", "", "", null); }
    private static String text(String value) { return value == null || value.isBlank() ? "" : value.trim(); }
}
