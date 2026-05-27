package dev.mrk.meshingress.controller.roles.registration;

import java.util.Locale;

final class ToolRegistrationIds {

    private ToolRegistrationIds() {
    }

    static String registrationId(ToolRegistrationRequest request) {
        return request.phase().wireName()
                + ":"
                + request.toolId()
                + ":"
                + Long.toUnsignedString(System.nanoTime(), 36);
    }

    static String canonicalToolId(String value) {
        if (value == null || value.isBlank()) {
            throw ToolRegistrationErrors.invalidParams("toolId is required.", "TOOL_REGISTRATION_TOOL_ID_REQUIRED");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
