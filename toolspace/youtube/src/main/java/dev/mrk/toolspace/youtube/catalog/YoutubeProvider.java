package dev.mrk.toolspace.youtube.catalog;

/** Provider boundaries; no client, credentials, or network implementation is selected by this foundation. */
public record YoutubeProvider(
        String id,
        String title,
        String kind,
        String authorization,
        String status,
        String notes
) {
}
