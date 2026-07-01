package dev.mrk.meshingress.controller.roles.registration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryToolRegistrationStore implements ToolRegistrationStore {

    private final Map<String, ToolRegistrationRecord> records = new LinkedHashMap<>();

    @Override
    public synchronized Optional<ToolRegistrationRecord> findActive(String toolId, ToolRegistrationPhase phase) {
        return records.values().stream()
                .filter(record -> record.toolId().equals(toolId))
                .filter(record -> record.phase() == phase)
                .filter(this::isActive)
                .findFirst();
    }

    @Override
    public synchronized List<ToolRegistrationRecord> findActive(String toolId) {
        return records.values().stream()
                .filter(record -> record.toolId().equals(toolId))
                .filter(this::isActive)
                .toList();
    }

    @Override
    public synchronized ToolRegistrationRecord saveActive(ToolRegistrationRecord record) {
        records.put(record.registrationId(), record);
        return record;
    }

    @Override
    public synchronized void markReplaced(String registrationId) {
        markStatus(registrationId, "replaced");
    }

    @Override
    public synchronized ToolRegistrationRecord markStatus(String registrationId, String status) {
        ToolRegistrationRecord current = records.get(registrationId);
        if (current != null) {
            ToolRegistrationRecord updated = current.replace(status);
            records.put(registrationId, updated);
            return updated;
        }
        throw ToolRegistrationErrors.invalidParams(
                "Tool registration record does not exist.",
                "TOOL_REGISTRATION_RECORD_NOT_FOUND"
        );
    }

    @Override
    public synchronized List<ToolRegistrationRecord> list() {
        return List.copyOf(new ArrayList<>(records.values()));
    }

    private boolean isActive(ToolRegistrationRecord record) {
        return switch (record.status()) {
            case "active", "reconciled", "installed-restart-required", "already-installed-restart-required" -> true;
            default -> false;
        };
    }
}
