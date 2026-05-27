package dev.mrk.meshingress.controller.roles.registration;

import java.util.Arrays;

public enum ToolRegistrationPhase {
    EXPERIMENTAL("experimental"),
    STAGING("staging"),
    BUNDLE("bundle"),
    NATIVE("native");

    private final String wireName;

    ToolRegistrationPhase(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static ToolRegistrationPhase fromWire(String value) {
        return Arrays.stream(values())
                .filter(phase -> phase.wireName.equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> ToolRegistrationErrors.invalidParams(
                        "Unsupported tool registration phase.",
                        "TOOL_REGISTRATION_PHASE_UNSUPPORTED"
                ));
    }
}
