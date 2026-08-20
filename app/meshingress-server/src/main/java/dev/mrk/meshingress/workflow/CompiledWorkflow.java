package dev.mrk.meshingress.workflow;

import java.util.List;
import java.util.Map;

/** Immutable execution indexes produced after validating a workflow definition. */
public record CompiledWorkflow(
        WorkflowDefinition definition,
        Map<String, WorkflowNode> nodesByRequestId,
        Map<String, List<WorkflowEdge>> outgoingByRequestId,
        Map<String, List<WorkflowEdge>> incomingByRequestId
) {
}
