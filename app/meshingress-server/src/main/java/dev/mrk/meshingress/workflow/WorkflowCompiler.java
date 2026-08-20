package dev.mrk.meshingress.workflow;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates the beta workflow contract and produces immutable graph indexes for execution. */
@Component
public class WorkflowCompiler {

    public CompiledWorkflow compile(WorkflowDefinition definition) {
        Map<String, WorkflowNode> nodes = indexNodes(definition.nodes());
        Map<String, String> resultOwners = indexResults(nodes);
        validateNodeContracts(nodes);
        validateInputReferences(nodes, resultOwners);

        Map<String, List<WorkflowEdge>> outgoing = new LinkedHashMap<>();
        Map<String, List<WorkflowEdge>> incoming = new LinkedHashMap<>();
        for (String requestId : nodes.keySet()) {
            outgoing.put(requestId, new ArrayList<>());
            incoming.put(requestId, new ArrayList<>());
        }
        for (WorkflowEdge edge : definition.edges()) {
            validateEdge(edge, nodes);
            outgoing.get(edge.from().requestId()).add(edge);
            incoming.get(edge.to().requestId()).add(edge);
        }

        validateTriggerCount(nodes);
        validateIncomingTopology(nodes, incoming);
        validateAcyclic(nodes, outgoing);
        validateReachability(nodes, outgoing);
        return new CompiledWorkflow(definition, Map.copyOf(nodes), freeze(outgoing), freeze(incoming));
    }

    private Map<String, WorkflowNode> indexNodes(List<WorkflowNode> nodeList) {
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        for (WorkflowNode node : nodeList) {
            if (nodes.putIfAbsent(node.requestId(), node) != null) {
                throw new WorkflowValidationException("Duplicate node requestId: " + node.requestId());
            }
        }
        if (nodes.isEmpty()) {
            throw new WorkflowValidationException("Workflow must contain at least one node");
        }
        return nodes;
    }

    private Map<String, String> indexResults(Map<String, WorkflowNode> nodes) {
        Map<String, String> owners = new HashMap<>();
        for (WorkflowNode node : nodes.values()) {
            String resultName = node.result().as();
            if (owners.putIfAbsent(resultName, node.requestId()) != null) {
                throw new WorkflowValidationException("Duplicate result name: " + resultName);
            }
        }
        return owners;
    }

    private void validateNodeContracts(Map<String, WorkflowNode> nodes) {
        for (WorkflowNode node : nodes.values()) {
            WorkflowResultDeclaration result = node.result();
            if (result.type() == WorkflowResultDeclaration.WorkflowValueType.BOOLEAN
                    && !(result.routing() instanceof WorkflowRouting.BooleanPorts)) {
                throw new WorkflowValidationException("Boolean result requires true and false ports: " + node.requestId());
            }
            if (result.type() != WorkflowResultDeclaration.WorkflowValueType.BOOLEAN
                    && !(result.routing() instanceof WorkflowRouting.Cases)) {
                throw new WorkflowValidationException("Non-boolean result requires cases and a default port: " + node.requestId());
            }
            if ((result.type() == WorkflowResultDeclaration.WorkflowValueType.OBJECT
                    || result.type() == WorkflowResultDeclaration.WorkflowValueType.ARRAY)
                    && result.routing() instanceof WorkflowRouting.Cases cases
                    && !cases.cases().isEmpty()
                    && cases.subjectPointer().isEmpty()) {
                throw new WorkflowValidationException("Object and array routing require a subject pointer: " + node.requestId());
            }
            if (node instanceof WorkflowNode.ExpressionTrue expression
                    && result.type() != WorkflowResultDeclaration.WorkflowValueType.BOOLEAN) {
                throw new WorkflowValidationException("expression.true must declare a boolean result: " + expression.requestId());
            }
        }
    }

    private void validateInputReferences(Map<String, WorkflowNode> nodes, Map<String, String> resultOwners) {
        Set<String> availableResults = new HashSet<>(resultOwners.keySet());
        for (WorkflowNode node : nodes.values()) {
            availableResults.add(node.failurePolicy().errorResultName());
        }
        for (WorkflowNode node : nodes.values()) {
            for (WorkflowInput input : inputsFor(node)) {
                if (input instanceof WorkflowInput.ResultReference reference && !availableResults.contains(reference.resultName())) {
                    throw new WorkflowValidationException("Unknown result reference: " + reference.resultName());
                }
            }
        }
    }

    private List<WorkflowInput> inputsFor(WorkflowNode node) {
        if (node instanceof WorkflowNode.ToolCall toolCall) {
            return List.copyOf(toolCall.arguments().values());
        }
        if (node instanceof WorkflowNode.Compose compose) {
            return List.copyOf(compose.fields().values());
        }
        if (node instanceof WorkflowNode.ExpressionTrue expression) {
            return List.of(expression.expression());
        }
        if (node instanceof WorkflowNode.Join join) {
            return List.copyOf(join.fields().values());
        }
        return List.of();
    }

    private void validateEdge(WorkflowEdge edge, Map<String, WorkflowNode> nodes) {
        WorkflowNode from = nodes.get(edge.from().requestId());
        WorkflowNode to = nodes.get(edge.to().requestId());
        if (from == null) {
            throw new WorkflowValidationException("Edge source requestId does not exist: " + edge.from().requestId());
        }
        if (to == null) {
            throw new WorkflowValidationException("Edge target requestId does not exist: " + edge.to().requestId());
        }
        if (!from.result().routing().ports().contains(edge.from().port())
                && !from.failurePolicy().errorPort().equals(edge.from().port())) {
            throw new WorkflowValidationException("Unknown source port " + edge.from().port() + " on " + from.requestId());
        }
        if (!"in".equals(edge.to().port())) {
            throw new WorkflowValidationException("The beta runtime supports only the input port named 'in': " + edge.to().requestId());
        }
    }

    private void validateTriggerCount(Map<String, WorkflowNode> nodes) {
        long triggerCount = nodes.values().stream().filter(WorkflowNode.ManualTrigger.class::isInstance).count();
        if (triggerCount != 1) {
            throw new WorkflowValidationException("The beta runtime requires exactly one manual trigger");
        }
    }

    private void validateIncomingTopology(Map<String, WorkflowNode> nodes, Map<String, List<WorkflowEdge>> incoming) {
        for (Map.Entry<String, List<WorkflowEdge>> entry : incoming.entrySet()) {
            WorkflowNode node = nodes.get(entry.getKey());
            int count = entry.getValue().size();
            if (node instanceof WorkflowNode.ManualTrigger && count != 0) {
                throw new WorkflowValidationException("Manual trigger cannot have incoming edges: " + node.requestId());
            }
            if (node instanceof WorkflowNode.Join && count < 2) {
                throw new WorkflowValidationException("Join requires at least two inbound edges: " + node.requestId());
            }
            if (!(node instanceof WorkflowNode.Merge) && !(node instanceof WorkflowNode.Join)
                    && !(node instanceof WorkflowNode.ManualTrigger) && count > 1) {
                throw new WorkflowValidationException("Multiple inbound edges require a merge or join node: " + node.requestId());
            }
        }
    }

    private void validateAcyclic(Map<String, WorkflowNode> nodes, Map<String, List<WorkflowEdge>> outgoing) {
        Map<String, Integer> indegree = new HashMap<>();
        for (String requestId : nodes.keySet()) {
            indegree.put(requestId, 0);
        }
        for (List<WorkflowEdge> edges : outgoing.values()) {
            for (WorkflowEdge edge : edges) {
                indegree.compute(edge.to().requestId(), (ignored, value) -> value + 1);
            }
        }
        ArrayDeque<String> ready = new ArrayDeque<>();
        indegree.forEach((requestId, count) -> {
            if (count == 0) {
                ready.add(requestId);
            }
        });
        int visited = 0;
        while (!ready.isEmpty()) {
            String requestId = ready.removeFirst();
            visited++;
            for (WorkflowEdge edge : outgoing.get(requestId)) {
                int remaining = indegree.compute(edge.to().requestId(), (ignored, value) -> value - 1);
                if (remaining == 0) {
                    ready.add(edge.to().requestId());
                }
            }
        }
        if (visited != nodes.size()) {
            throw new WorkflowValidationException("Cycles require a future loop node and are not supported by the beta runtime");
        }
    }

    private void validateReachability(Map<String, WorkflowNode> nodes, Map<String, List<WorkflowEdge>> outgoing) {
        String triggerRequestId = nodes.values().stream()
                .filter(WorkflowNode.ManualTrigger.class::isInstance)
                .map(WorkflowNode::requestId)
                .findFirst()
                .orElseThrow(() -> new WorkflowValidationException("Workflow must contain a manual trigger"));
        Set<String> reachable = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        reachable.add(triggerRequestId);
        pending.add(triggerRequestId);
        while (!pending.isEmpty()) {
            for (WorkflowEdge edge : outgoing.get(pending.removeFirst())) {
                if (reachable.add(edge.to().requestId())) {
                    pending.add(edge.to().requestId());
                }
            }
        }
        Set<String> unreachable = new HashSet<>(nodes.keySet());
        unreachable.removeAll(reachable);
        if (!unreachable.isEmpty()) {
            throw new WorkflowValidationException("Every node must be reachable from the manual trigger: " + unreachable);
        }
    }

    private Map<String, List<WorkflowEdge>> freeze(Map<String, List<WorkflowEdge>> source) {
        Map<String, List<WorkflowEdge>> frozen = new LinkedHashMap<>();
        source.forEach((requestId, edges) -> frozen.put(requestId, List.copyOf(edges)));
        return Map.copyOf(frozen);
    }
}
