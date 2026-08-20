package dev.mrk.meshingress.workflow;

import java.util.List;
import java.util.Objects;

/** A layout-free, executable workflow definition. */
public record WorkflowDefinition(
        String id,
        List<WorkflowNode> nodes,
        List<WorkflowEdge> edges
) {
    public WorkflowDefinition {
        requireText(id, "workflow id");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes must not be null"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));
    }

    static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
    }
}
