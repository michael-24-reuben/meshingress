package dev.mrk.meshingress.controller.roles.registration;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
class InMemoryToolRegistrationStore implements ToolRegistrationStore {

    private final Map<String, ToolRegistrationRecord> records = new LinkedHashMap<>();

    @Override
    public synchronized Optional<ToolRegistrationRecord> findActive(String toolId, ToolRegistrationPhase phase) {
        return records.values().stream()
                .filter(record -> record.toolId().equals(toolId))
                .filter(record -> record.phase() == phase)
                .filter(record -> "active".equals(record.status()) || "reconciled".equals(record.status()))
                .findFirst();
    }

    @Override
    public synchronized List<ToolRegistrationRecord> findActive(String toolId) {
        return records.values().stream()
                .filter(record -> record.toolId().equals(toolId))
                .filter(record -> "active".equals(record.status()) || "reconciled".equals(record.status()))
                .toList();
    }

    @Override
    public synchronized ToolRegistrationRecord saveActive(ToolRegistrationRecord record) {
        records.put(record.registrationId(), record);
        return record;
    }

    @Override
    public synchronized void markReplaced(String registrationId) {
        ToolRegistrationRecord current = records.get(registrationId);
        if (current != null) {
            records.put(registrationId, current.replace("replaced"));
        }
    }

    @Override
    public synchronized List<ToolRegistrationRecord> list() {
        return List.copyOf(new ArrayList<>(records.values()));
    }
}
