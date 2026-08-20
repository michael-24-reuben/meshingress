package dev.mrk.toolspace.youtube.catalog;

/** A stable planned MCP function name and the boundary that will implement it. */
public record YoutubeCapability(
        String function,
        String title,
        String provider,
        String authorization,
        String status,
        String description
) {
}
