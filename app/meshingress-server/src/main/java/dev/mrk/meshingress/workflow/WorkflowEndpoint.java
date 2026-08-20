package dev.mrk.meshingress.workflow;

/** A graph endpoint addressed by a stable definition-level request ID and port. */
public record WorkflowEndpoint(String requestId, String port) {
    public WorkflowEndpoint {
        WorkflowDefinition.requireText(requestId, "endpoint requestId");
        WorkflowDefinition.requireText(port, "endpoint port");
    }
}
