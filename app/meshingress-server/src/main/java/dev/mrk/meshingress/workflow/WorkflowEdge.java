package dev.mrk.meshingress.workflow;

import java.util.Objects;

/** A control-flow edge. Data is read through individual result bindings, not copied across edges. */
public record WorkflowEdge(WorkflowEndpoint from, WorkflowEndpoint to) {
    public WorkflowEdge {
        from = Objects.requireNonNull(from, "from must not be null");
        to = Objects.requireNonNull(to, "to must not be null");
    }
}
