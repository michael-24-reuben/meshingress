package dev.mrk.meshingress.controller.roles.registration;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ToolRegistrationRecord(
        String registrationId,
        String toolId,
        ToolRegistrationPhase phase,
        ToolSourceKind sourceKind,
        String status,
        Map<String, String> source,
        String actor,
        String requestId,
        OffsetDateTime registeredAt,
        String replacedRegistrationId,
        String runtimeModuleId,
        List<String> registeredFunctions
) {
    public ToolRegistrationRecord {
        source = source == null ? Map.of() : Map.copyOf(source);
        registeredFunctions = registeredFunctions == null ? List.of() : List.copyOf(registeredFunctions);
    }

    ToolRegistrationRecord replace(String status) {
        return new ToolRegistrationRecord(
                registrationId,
                toolId,
                phase,
                sourceKind,
                status,
                source,
                actor,
                requestId,
                registeredAt,
                replacedRegistrationId,
                runtimeModuleId,
                registeredFunctions
        );
    }
}
