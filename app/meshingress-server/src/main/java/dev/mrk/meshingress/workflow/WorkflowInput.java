package dev.mrk.meshingress.workflow;

import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

/** A literal JSON value or a JSON Pointer reference to a previously individual result. */
public sealed interface WorkflowInput permits WorkflowInput.Literal, WorkflowInput.ResultReference {

    JsonNode resolve(Map<String, JsonNode> results);

    record Literal(JsonNode value) implements WorkflowInput {
        public Literal {
            value = Objects.requireNonNull(value, "literal value must not be null").deepCopy();
        }

        @Override
        public JsonNode resolve(Map<String, JsonNode> results) {
            return value.deepCopy();
        }
    }

    record ResultReference(String resultName, String pointer) implements WorkflowInput {
        public ResultReference {
            WorkflowDefinition.requireText(resultName, "result name");
            pointer = pointer == null ? "" : pointer;
            if (!pointer.isEmpty() && !pointer.startsWith("/")) {
                throw new IllegalArgumentException("result pointer must be empty or start with '/'");
            }
        }

        @Override
        public JsonNode resolve(Map<String, JsonNode> results) {
            JsonNode result = results.get(resultName);
            if (result == null) {
                throw new WorkflowValidationException("Result is not available: " + resultName);
            }
            JsonNode selected = pointer.isEmpty() ? result : result.at(pointer);
            if (selected.isMissingNode()) {
                throw new WorkflowValidationException("Result pointer does not exist: " + resultName + "#" + pointer);
            }
            return selected.deepCopy();
        }
    }
}
