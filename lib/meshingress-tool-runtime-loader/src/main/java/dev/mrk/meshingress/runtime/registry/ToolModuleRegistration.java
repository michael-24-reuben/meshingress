package dev.mrk.meshingress.runtime.registry;

import java.util.List;

public record ToolModuleRegistration(List<String> registeredFunctions) {
    public ToolModuleRegistration {
        registeredFunctions = registeredFunctions == null ? List.of() : List.copyOf(registeredFunctions);
    }
}
