package dev.mrk.toolspace.openinklibrary;

import java.util.Locale;

public class Utility {
    public static String normalize(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new SourceException("UNKNOWN_BOOK_SOURCE", "A non-blank source ID is required.");
        }
        return sourceId.trim().toLowerCase(Locale.ROOT);
    }

}
