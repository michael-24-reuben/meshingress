package dev.mrk.meshingress.runtime.lifecycle;

import java.time.OffsetDateTime;
import java.util.List;

public record ToolModuleHandle(
        ToolModuleId moduleId,
        ToolModuleState state,
        OffsetDateTime activatedAt,
        List<String> registeredFunctions
) {
    public ToolModuleHandle {
        registeredFunctions = registeredFunctions == null ? List.of() : List.copyOf(registeredFunctions);
    }
}
