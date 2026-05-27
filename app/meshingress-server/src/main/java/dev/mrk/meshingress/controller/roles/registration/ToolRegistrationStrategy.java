package dev.mrk.meshingress.controller.roles.registration;

public interface ToolRegistrationStrategy {

    ToolRegistrationPhase phase();

    ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context);
}
