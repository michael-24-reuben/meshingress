package dev.mrk.meshingress.schema.constraint;

import tools.jackson.databind.node.ObjectNode;

/** Common JSON Schema text constraints shared by textual MCP input categories. */
public class McpTextSchemaConstraint implements McpInputSchemaConstraint {

    private final int minLength;
    private final int maxLength;
    private final String pattern;
    private final String defaultValue;
    private final boolean hasDefault;

    public McpTextSchemaConstraint(int minLength, int maxLength, String pattern, String defaultValue, boolean hasDefault) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.pattern = pattern;
        this.defaultValue = defaultValue;
        this.hasDefault = hasDefault;
    }

    @Override
    public void applyTo(ObjectNode schema) {
        if (minLength >= 0) {
            schema.put("minLength", minLength);
        }
        if (maxLength >= 0) {
            schema.put("maxLength", maxLength);
        }
        if (pattern != null && !pattern.isBlank()) {
            schema.put("pattern", pattern);
        }
        if (hasDefault) {
            schema.put("default", defaultValue);
        }
    }
}
