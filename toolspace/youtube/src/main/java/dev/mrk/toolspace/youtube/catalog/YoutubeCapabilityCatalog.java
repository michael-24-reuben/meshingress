package dev.mrk.toolspace.youtube.catalog;

import java.util.List;

/**
 * Catalogs the callable YouTube MCP surface and reserved follow-up capabilities without
 * presenting future OAuth, transcript, creator, or media work as implemented.
 */
public final class YoutubeCapabilityCatalog {
    private static final String AVAILABLE = "available";
    private static final String PLANNED = "planned";

    public List<YoutubeCapability> capabilities() {
        return List.of(
                capability("youtube.catalog.capabilities", "List YouTube capabilities", "foundation", "none", AVAILABLE, "Returns this catalog."),
                capability("youtube.catalog.providers", "List YouTube providers", "foundation", "none", AVAILABLE, "Returns provider boundaries and delivery state."),
                capability("youtube.data.search", "Search YouTube", "official-data-api", "API key", AVAILABLE, "Search public videos, channels, or playlists through the configured official Data API client."),
                capability("youtube.data.videos-get", "Get videos", "official-data-api", "API key", AVAILABLE, "Read public video metadata, status, statistics, and live details."),
                capability("youtube.data.channels-get", "Get channels", "official-data-api", "API key", AVAILABLE, "Read public channel metadata, uploads playlist, statistics, and branding."),
                capability("youtube.data.playlists-get", "Get playlists", "official-data-api", "API key", AVAILABLE, "Read public playlist metadata."),
                capability("youtube.data.playlist-items-list", "List playlist items", "official-data-api", "API key or OAuth by requested fields", PLANNED, "Read ordered playlist contents."),
                capability("youtube.data.comments-list", "List comments", "official-data-api", "API key or OAuth by requested fields", PLANNED, "Read comment threads and replies where available."),
                capability("youtube.data.captions-list", "List captions", "official-data-api", "OAuth", PLANNED, "List caption tracks that the authorized account may access."),
                capability("youtube.data.subscriptions-list", "List subscriptions", "official-data-api", "OAuth", PLANNED, "List subscriptions for the authorized channel."),
                capability("youtube.data.live-broadcasts-list", "List live broadcasts", "official-data-api", "OAuth", PLANNED, "Read broadcasts and streams for the authorized channel."),
                capability("youtube.data.live-chat-messages-list", "List live chat messages", "official-data-api", "OAuth", PLANNED, "Read messages for an authorized live chat."),
                capability("youtube.creator.video-upload", "Upload video", "official-data-api", "OAuth write scope", PLANNED, "Resumable upload and initial video metadata."),
                capability("youtube.creator.video-update", "Update video", "official-data-api", "OAuth write scope", PLANNED, "Update owned video metadata and status."),
                capability("youtube.creator.video-delete", "Delete video", "official-data-api", "OAuth write scope", PLANNED, "Delete an owned video after explicit destructive-action policy."),
                capability("youtube.creator.playlist-create", "Create playlist", "official-data-api", "OAuth write scope", PLANNED, "Create an owned playlist."),
                capability("youtube.creator.playlist-update", "Update playlist", "official-data-api", "OAuth write scope", PLANNED, "Update an owned playlist."),
                capability("youtube.creator.playlist-delete", "Delete playlist", "official-data-api", "OAuth write scope", PLANNED, "Delete an owned playlist after explicit destructive-action policy."),
                capability("youtube.creator.thumbnail-set", "Set thumbnail", "official-data-api", "OAuth write scope", PLANNED, "Set an owned video's thumbnail."),
                capability("youtube.creator.comment-moderate", "Moderate comment", "official-data-api", "OAuth write scope", PLANNED, "Moderate or remove comments owned by the authorized channel."),
                capability("youtube.analytics.query", "Query analytics", "official-analytics-api", "OAuth analytics scope", PLANNED, "Run targeted metrics, dimensions, filters, and date-range queries."),
                capability("youtube.reporting.jobs-list", "List reporting jobs", "official-reporting-api", "OAuth reporting scope", PLANNED, "Discover bulk-reporting jobs before download support is added."),
                capability("youtube.transcript.get", "Get transcript", "dedicated-transcript-provider", "provider-specific", PLANNED, "Obtain a transcript only through a later selected, policy-compliant provider."),
                capability("youtube.transcript.search", "Search transcript", "dedicated-transcript-provider", "provider-specific", PLANNED, "Search transcript segments after transcript acquisition is implemented."),
                capability("youtube.ai.summarize", "Summarize video", "ai-orchestrator", "provider-specific", PLANNED, "Generate a summary from an acquired transcript."),
                capability("youtube.ai.chapters-propose", "Propose chapters", "ai-orchestrator", "provider-specific", PLANNED, "Propose chapters from an acquired transcript."),
                capability("youtube.media.inspect", "Inspect media", "media.ytdlp", "local tool policy", PLANNED, "Delegate only to the existing constrained local media companion; no downloader is duplicated here.")
        );
    }

    public List<YoutubeProvider> providers() {
        return List.of(
                provider("foundation", "YouTube foundation", "in-process catalog", "none", AVAILABLE, "Catalog and provider contracts only; makes no external calls."),
                provider("official-data-api", "YouTube Data API", "official Google API", "API key for current public reads", AVAILABLE, "Current public search, video, channel, and playlist reads use the configured API key. OAuth-gated reads and creator operations remain planned."),
                provider("official-analytics-api", "YouTube Analytics API", "official Google API", "OAuth", PLANNED, "Future targeted channel or content-owner reporting."),
                provider("official-reporting-api", "YouTube Reporting API", "official Google API", "OAuth", PLANNED, "Future bulk report lifecycle and retrieval."),
                provider("dedicated-transcript-provider", "Transcript provider", "selected dedicated provider", "provider-specific", PLANNED, "Provider selection and policy review are deferred; the official Data API is not treated as transcript text access."),
                provider("ai-orchestrator", "AI orchestration", "Meshingress composition", "provider-specific", PLANNED, "Consumes acquired transcript content; it is not a YouTube source."),
                provider("media.ytdlp", "Local media companion", "existing Meshingress tool", "local tool policy", AVAILABLE, "Existing constrained inspect/download capability remains the sole media-download boundary.")
        );
    }

    private static YoutubeCapability capability(String function, String title, String provider, String authorization, String status, String description) {
        return new YoutubeCapability(function, title, provider, authorization, status, description);
    }

    private static YoutubeProvider provider(String id, String title, String kind, String authorization, String status, String notes) {
        return new YoutubeProvider(id, title, kind, authorization, status, notes);
    }
}
