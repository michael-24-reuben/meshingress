package dev.mrk.toolspace.openinklibrary;

import java.util.Locale;

public enum PublicationType {
    NOVEL,
    LIGHT_NOVEL,
    MANGA,
    MANHWA,
    MANHUA,
    WEBTOON,
    COMIC,
    GRAPHIC_NOVEL,
    UNKNOWN;

    public static PublicationType fromSourceValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
