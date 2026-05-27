package dev.mrk.meshingress.runtime.lifecycle;

import java.util.Objects;

public record ToolModuleId(String value) {

    public ToolModuleId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Tool module id must not be blank");
        }
    }

    public static ToolModuleId of(String groupId, String artifactId, String version) {
        return new ToolModuleId(groupId + ":" + artifactId + ":" + version);
    }
}
