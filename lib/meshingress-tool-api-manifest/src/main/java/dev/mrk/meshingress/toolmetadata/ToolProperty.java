package dev.mrk.meshingress.toolmetadata;

public record ToolProperty(
        String name,
        String description,
        String defaultValue,
        String valueType,
        boolean required,
        boolean secret
) {
    public ToolProperty {
        name = normalizeName(name);
        description = description == null ? "" : description.strip();
        defaultValue = defaultValue == null ? "" : defaultValue;
        valueType = valueType == null || valueType.isBlank() ? "string" : valueType.strip();
    }

    public static ToolProperty string(String name) {
        return new ToolProperty(name, "", "", "string", false, false);
    }

    public static ToolProperty string(String name, String defaultValue) {
        return new ToolProperty(name, "", defaultValue, "string", false, false);
    }

    public static ToolProperty longValue(String name) {
        return new ToolProperty(name, "", "", "long", false, false);
    }

    public static ToolProperty longValue(String name, Long defaultValue) {
        return new ToolProperty(name, "", defaultValue == null ? "" : defaultValue.toString(), "long", false, false);
    }

    public static ToolProperty booleanValue(String name) {
        return new ToolProperty(name, "", "", "boolean", false, false);
    }

    public static ToolProperty booleanValue(String name, Boolean defaultValue) {
        return new ToolProperty(name, "", defaultValue == null ? "" : defaultValue.toString(), "boolean", false, false);
    }

    public static ToolProperty secretRef(String name) {
        return new ToolProperty(name, "", "", "secret-ref", true, true);
    }

    public static ToolProperty secretRef(String name, String defaultValue) {
        return new ToolProperty(name, "", defaultValue, "secret-ref", true, true);
    }

    public ToolProperty description(String value) {
        return new ToolProperty(name, value, defaultValue, valueType, required, secret);
    }

    public ToolProperty defaultValue(Object value) {
        return new ToolProperty(name, description, value == null ? "" : value.toString(), valueType, required, secret);
    }

    public ToolProperty valueType(String value) {
        return new ToolProperty(name, description, defaultValue, value, required, secret);
    }

    public ToolProperty required(boolean value) {
        return new ToolProperty(name, description, defaultValue, valueType, value, secret);
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
