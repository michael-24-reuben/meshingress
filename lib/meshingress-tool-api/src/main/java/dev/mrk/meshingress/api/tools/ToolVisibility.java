package dev.mrk.meshingress.api.tools;

import java.util.Map;

public enum ToolVisibility {
    PUBLIC,
    PRIVATE,
    ADMIN;

    public static ToolVisibility fromWire(String value) {
        if (value == null || value.isBlank()) {
            return PUBLIC;
        }
        return ToolVisibility.valueOf(value.trim().toUpperCase());
    }

    public String toWire() {
        return name().toLowerCase();
    }

    public boolean canContainVisibility(ToolVisibility methodVisibility) {
        if (methodVisibility == null) {
            return false;
        }

        return switch (this) {
            case ADMIN -> true;
            case PRIVATE -> methodVisibility == PRIVATE;
            case PUBLIC -> methodVisibility == PUBLIC || methodVisibility == PRIVATE;
        };
    }
}
