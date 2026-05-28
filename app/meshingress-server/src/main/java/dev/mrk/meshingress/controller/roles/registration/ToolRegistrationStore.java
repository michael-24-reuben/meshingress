package dev.mrk.meshingress.controller.roles.registration;

import java.util.List;
import java.util.Optional;

public interface ToolRegistrationStore {

    Optional<ToolRegistrationRecord> findActive(String toolId, ToolRegistrationPhase phase);

    List<ToolRegistrationRecord> findActive(String toolId);

    ToolRegistrationRecord saveActive(ToolRegistrationRecord record);

    void markReplaced(String registrationId);

    ToolRegistrationRecord markStatus(String registrationId, String status);

    List<ToolRegistrationRecord> list();
}
