package dev.mrk.meshingress.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Native workflow nodes and the dedicated bridge node for registered MCP tool functions. */
public sealed interface WorkflowNode permits WorkflowNode.ManualTrigger, WorkflowNode.ToolCall,
        WorkflowNode.Compose, WorkflowNode.ExpressionTrue, WorkflowNode.Merge, WorkflowNode.Join {

    String requestId();

    WorkflowResultDeclaration result();

    default WorkflowFailurePolicy failurePolicy() {
        return WorkflowFailurePolicy.failWorkflow();
    }

    record ManualTrigger(String requestId, WorkflowResultDeclaration result) implements WorkflowNode {
        public ManualTrigger {
            WorkflowDefinition.requireText(requestId, "node requestId");
            result = Objects.requireNonNull(result, "result must not be null");
        }
    }

    record ToolCall(
            String requestId,
            String functionName,
            Map<String, WorkflowInput> arguments,
            WorkflowResultDeclaration result,
            WorkflowFailurePolicy failurePolicy
    ) implements WorkflowNode {
        public ToolCall {
            WorkflowDefinition.requireText(requestId, "node requestId");
            WorkflowDefinition.requireText(functionName, "tool function name");
            arguments = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(arguments, "arguments must not be null")));
            result = Objects.requireNonNull(result, "result must not be null");
            failurePolicy = Objects.requireNonNull(failurePolicy, "failure policy must not be null");
        }
    }

    record Compose(
            String requestId,
            Map<String, WorkflowInput> fields,
            WorkflowResultDeclaration result,
            WorkflowFailurePolicy failurePolicy
    ) implements WorkflowNode {
        public Compose {
            WorkflowDefinition.requireText(requestId, "node requestId");
            fields = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(fields, "fields must not be null")));
            result = Objects.requireNonNull(result, "result must not be null");
            failurePolicy = Objects.requireNonNull(failurePolicy, "failure policy must not be null");
        }
    }

    record ExpressionTrue(
            String requestId,
            WorkflowInput expression,
            WorkflowResultDeclaration result,
            WorkflowFailurePolicy failurePolicy
    ) implements WorkflowNode {
        public ExpressionTrue {
            WorkflowDefinition.requireText(requestId, "node requestId");
            expression = Objects.requireNonNull(expression, "expression must not be null");
            result = Objects.requireNonNull(result, "result must not be null");
            failurePolicy = Objects.requireNonNull(failurePolicy, "failure policy must not be null");
        }
    }

    record Merge(String requestId, WorkflowResultDeclaration result) implements WorkflowNode {
        public Merge {
            WorkflowDefinition.requireText(requestId, "node requestId");
            result = Objects.requireNonNull(result, "result must not be null");
        }
    }

    record Join(
            String requestId,
            Map<String, WorkflowInput> fields,
            WorkflowResultDeclaration result
    ) implements WorkflowNode {
        public Join {
            WorkflowDefinition.requireText(requestId, "node requestId");
            fields = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(fields, "fields must not be null")));
            result = Objects.requireNonNull(result, "result must not be null");
        }
    }
}
