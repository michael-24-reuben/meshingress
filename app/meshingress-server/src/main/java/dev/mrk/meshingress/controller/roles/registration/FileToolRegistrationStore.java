package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
class FileToolRegistrationStore implements ToolRegistrationStore {

    private static final int SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final Path storePath;
    private final Map<String, ToolRegistrationRecord> records = new LinkedHashMap<>();

    FileToolRegistrationStore(ObjectMapper objectMapper, MeshingressProperties properties) {
        this.objectMapper = objectMapper;
        this.storePath = resolveStorePath(properties).toAbsolutePath().normalize();
        load();
    }

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
        persist();
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
            persist();
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

    private void load() {
        if (!Files.isRegularFile(storePath)) {
            return;
        }
        try {
            StoreDocument document = objectMapper.readValue(Files.readString(storePath, StandardCharsets.UTF_8), StoreDocument.class);
            if (document.schemaVersion() != SCHEMA_VERSION) {
                throw new IllegalStateException("Unsupported tool registration store schema: " + document.schemaVersion());
            }
            for (ToolRegistrationRecord record : document.records()) {
                records.put(record.registrationId(), record);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load tool registration store: " + storePath, exception);
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storePath.getParent());
            Path temporary = storePath.resolveSibling(storePath.getFileName() + ".tmp");
            String json = objectMapper.writeValueAsString(new StoreDocument(SCHEMA_VERSION, List.copyOf(records.values())));
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, storePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist tool registration store: " + storePath, exception);
        }
    }

    private Path resolveStorePath(MeshingressProperties properties) {
        Path configured = Path.of(properties.repository().runtimeRegistrationStorePath());
        if (configured.isAbsolute()) {
            return configured;
        }
        return Path.of(properties.repository().root()).resolve(configured);
    }

    private boolean isActive(ToolRegistrationRecord record) {
        return switch (record.status()) {
            case "active", "reconciled", "installed-restart-required", "already-installed-restart-required" -> true;
            default -> false;
        };
    }

    private record StoreDocument(int schemaVersion, List<ToolRegistrationRecord> records) {
        private StoreDocument {
            records = records == null ? List.of() : List.copyOf(records);
        }
    }
}
