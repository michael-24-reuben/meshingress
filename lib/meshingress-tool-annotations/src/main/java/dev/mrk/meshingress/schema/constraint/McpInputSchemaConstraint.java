package dev.mrk.meshingress.schema.constraint;

import tools.jackson.databind.node.ObjectNode;

/** A reusable compiled input constraint that contributes standard JSON Schema keywords. */
public interface McpInputSchemaConstraint {

    void applyTo(ObjectNode schema);
}
