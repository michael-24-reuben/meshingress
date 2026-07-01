package dev.mrk.meshingress.artifact.security;

import java.util.Locale;

public enum GrypeSeverity {
    INFO(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int rank;

    GrypeSeverity(int rank) {
        this.rank = rank;
    }

    public boolean atLeast(GrypeSeverity threshold) {
        GrypeSeverity safeThreshold = threshold == null ? CRITICAL : threshold;
        return rank >= safeThreshold.rank;
    }

    public int rank() {
        return rank;
    }

    public static GrypeSeverity fromWire(String value, GrypeSeverity fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? INFO : fallback;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if (normalized.equals("UNKNOWN") || normalized.equals("NEGLIGIBLE")) {
            return INFO;
        }
        try {
            return GrypeSeverity.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return fallback == null ? INFO : fallback;
        }
    }
}
