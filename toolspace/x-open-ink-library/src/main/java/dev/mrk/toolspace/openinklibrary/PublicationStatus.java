package dev.mrk.toolspace.openinklibrary;

import java.util.Locale;

public enum PublicationStatus {
    ONGOING,
    COMPLETED,
    HIATUS,
    CANCELLED,
    UNKNOWN;

    public static PublicationStatus fromSourceValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ONGOING", "RELEASING" -> ONGOING;
            case "COMPLETED", "FINISHED" -> COMPLETED;
            case "HIATUS", "ON_HIATUS" -> HIATUS;
            case "CANCELLED", "CANCELED" -> CANCELLED;
            default -> UNKNOWN;
        };
    }
}
