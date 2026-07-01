package dev.mrk.meshingress.toolmetadata;

public record McpToolPropertyMetadata(
        String name,
        String description,
        String defaultValue,
        String valueType,
        boolean required,
        String assignedValue
) {
    public McpToolPropertyMetadata {
        name = normalizeName(name);
        description = description == null ? "" : description.strip();
        defaultValue = defaultValue == null ? "" : defaultValue;
        valueType = valueType == null || valueType.isBlank() ? "string" : valueType.strip();
        assignedValue = assignedValue == null ? "" : assignedValue;
    }

    public String value() {
        return assignedValue.isBlank() ? defaultValue : assignedValue;
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tool property name must not be blank");
        }
        String normalized = value.strip();
        if (normalized.startsWith(".") || normalized.endsWith(".") || normalized.contains("..")) {
            throw new IllegalArgumentException("tool property name must use non-empty dotted segments: " + value);
        }
        return normalized;
    }
}
