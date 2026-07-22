package dev.mrk.toolspace.openinklibrary.source.toonverse;

import java.net.URI;
import java.time.Duration;

public record ToonverseSourceConfiguration(
        String id,
        String displayName,
        URI apiBaseUrl,
        String metadataPath,
        String metadataSelector,
        String nameSearchPath,
        String nameSearchSelector,
        String searchPath,
        String searchSelector,
        String readingChapterPath,
        String readingChapterSelector,
        String chaptersPath,
        String chaptersSelector,
        Duration timeout,
        AuthorizationMode authorizationMode,
        int defaultSearchLimit,
        int maximumSearchLimit,
        int defaultChapterLimit,
        int maximumChapterLimit,
        String defaultChapterOrder
) {
    public enum AuthorizationMode {
        NONE,
        OPTIONAL_BEARER,
        BEARER
    }
}
