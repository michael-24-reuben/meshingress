package dev.mrk.meshingress.runtime.lifecycle;

import java.time.OffsetDateTime;
import java.util.List;

public record ToolModuleStatus(
        ToolModuleId moduleId,
        ToolModuleState state,
        OffsetDateTime activatedAt,
        OffsetDateTime updatedAt,
        List<String> registeredFunctions,
        String message
) {
    public ToolModuleStatus {
        registeredFunctions = registeredFunctions == null ? List.of() : List.copyOf(registeredFunctions);
    }
}
