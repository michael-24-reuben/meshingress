package dev.mrk.meshingress.workflow.web;

import dev.mrk.meshingress.workflow.WorkflowDefinition;
import dev.mrk.meshingress.workflow.WorkflowEdge;
import dev.mrk.meshingress.workflow.WorkflowEndpoint;
import dev.mrk.meshingress.workflow.WorkflowFailurePolicy;
import dev.mrk.meshingress.workflow.WorkflowInput;
import dev.mrk.meshingress.workflow.WorkflowNode;
import dev.mrk.meshingress.workflow.WorkflowResultDeclaration;
import dev.mrk.meshingress.workflow.WorkflowRouting;
import dev.mrk.meshingress.workflow.WorkflowValidationException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Ephemeral Studio graph payload translated to the executable workflow model; it is never persisted. */
public record StudioWorkflowDefinition(
        String id,
        Integer version,
        List<StudioWorkflowNode> nodes,
        List<StudioWorkflowEdge> edges
) {
    public StudioWorkflowDefinition {
        requireText(id, "workflow id");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes must not be null"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));
    }

    public WorkflowDefinition executableDefinition() {
        Map<String, StudioWorkflowNode> nodesById = new LinkedHashMap<>();
        for (StudioWorkflowNode node : nodes) {
            if (nodesById.putIfAbsent(node.requestId(), node) != null) {
                throw new WorkflowValidationException("Duplicate node requestId: " + node.requestId());
            }
        }
        StudioWorkflowNode trigger = nodesById.values().stream()
                .filter(node -> "trigger".equals(node.kind()))
                .findFirst()
                .orElseThrow(() -> new WorkflowValidationException("Workflow must contain a manual trigger"));
        if (nodesById.values().stream().filter(node -> "trigger".equals(node.kind())).count() != 1) {
            throw new WorkflowValidationException("The Studio runtime requires exactly one manual trigger");
        }
        for (StudioWorkflowEdge edge : edges) {
            if (!nodesById.containsKey(edge.source()) || !nodesById.containsKey(edge.target())) {
                throw new WorkflowValidationException("Edge endpoints must reference Studio nodes");
            }
        }

        Set<String> activeIds = reachableFrom(trigger.requestId());
        List<WorkflowNode> activeNodes = nodes.stream()
                .filter(node -> activeIds.contains(node.requestId()))
                .map(StudioWorkflowNode::toWorkflowNode)
                .toList();
        List<WorkflowEdge> activeEdges = edges.stream()
                .filter(edge -> activeIds.contains(edge.source()) && activeIds.contains(edge.target()))
                .map(edge -> new WorkflowEdge(new WorkflowEndpoint(edge.source(), "default"), new WorkflowEndpoint(edge.target(), "in")))
                .toList();
        return new WorkflowDefinition(id, activeNodes, activeEdges);
    }

    private Set<String> reachableFrom(String triggerId) {
        Set<String> reachable = new LinkedHashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(triggerId);
        while (!pending.isEmpty()) {
            String source = pending.removeFirst();
            if (!reachable.add(source)) {
                continue;
            }
            edges.stream()
                    .filter(edge -> edge.source().equals(source))
                    .map(StudioWorkflowEdge::target)
                    .forEach(pending::addLast);
        }
        return reachable;
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
    }

    public record StudioWorkflowNode(
            String requestId,
            String kind,
            String functionName,
            Map<String, JsonNode> arguments,
        String output
    ) {
        public StudioWorkflowNode {
            requireText(requestId, "node requestId");
            requireText(kind, "node kind");
            arguments = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(arguments, "arguments must not be null")));
            requireText(output, "node output");
        }

        private WorkflowNode toWorkflowNode() {
            WorkflowResultDeclaration result = "trigger".equals(kind)
                    ? new WorkflowResultDeclaration(output, WorkflowResultDeclaration.WorkflowValueType.NULL, defaultRouting())
                    : new WorkflowResultDeclaration(output, WorkflowResultDeclaration.WorkflowValueType.OBJECT, defaultRouting());
            if ("trigger".equals(kind)) {
                return new WorkflowNode.ManualTrigger(requestId, result);
            }
            if (!"tool".equals(kind)) {
                throw new WorkflowValidationException("Unsupported Studio node kind: " + kind);
            }
            requireText(functionName, "tool function name");
            Map<String, WorkflowInput> inputs = new LinkedHashMap<>();
            arguments.forEach((name, value) -> inputs.put(name, new WorkflowInput.Literal(value)));
            return new WorkflowNode.ToolCall(requestId, functionName, inputs, result, WorkflowFailurePolicy.failWorkflow());
        }

        private WorkflowRouting.Cases defaultRouting() {
            return new WorkflowRouting.Cases("", List.of(), "default");
        }
    }

    public record StudioWorkflowEdge(String source, String target) {
        public StudioWorkflowEdge {
            requireText(source, "edge source");
            requireText(target, "edge target");
        }
    }
}
