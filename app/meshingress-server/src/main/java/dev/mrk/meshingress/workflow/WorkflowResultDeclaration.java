package dev.mrk.meshingress.workflow;

import java.util.Objects;

/** Names a node result in execution context and defines its expected JSON type and route ports. */
public record WorkflowResultDeclaration(
        String as,
        WorkflowValueType type,
        WorkflowRouting routing
) {
    public WorkflowResultDeclaration {
        WorkflowDefinition.requireText(as, "result name");
        type = Objects.requireNonNull(type, "result type must not be null");
        routing = Objects.requireNonNull(routing, "routing must not be null");
    }

    public enum WorkflowValueType {
        BOOLEAN,
        STRING,
        NUMBER,
        OBJECT,
        ARRAY,
        NULL
    }
}
