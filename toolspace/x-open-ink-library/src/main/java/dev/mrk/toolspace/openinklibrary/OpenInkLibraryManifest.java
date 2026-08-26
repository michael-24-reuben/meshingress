package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolModuleMetadata;
import dev.mrk.meshingress.toolmetadata.ToolReadme;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;

import java.util.List;

/** Static module metadata for the source-owned Open Ink Library tool family. */
public final class OpenInkLibraryManifest implements McpToolManifestDefinition {

    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "open-ink-library",
                "Open Ink Library",
                "Public comic and serialized-fiction source tools",
                "Provides source-owned MCP tools for discovering and retrieving public webtoon, manga, manhwa, comic, and serialized-fiction data.",
                List.of(),
                "",
                List.of("books", "comics", "manga", "manhwa", "webtoon", "toonverse"),
                List.of(),
                null
        );
    }

    @Override
    public List<ToolRequirement> requirements() {
        return List.of(
                ToolRequirement.externalApi("Toonverse public API")
                        .description("Public source API used by the currently installed Toonverse adapter."),
                ToolRequirement.scope(McpToolScope.HTTP_CLIENT),
                ToolRequirement.scope(McpToolScope.EXTERNAL_API_READ)
        );
    }

    @Override
    public ToolReadme readme() {
        return ToolReadme.inline("""
                # Open Ink Library

                Open Ink Library exposes source-owned public reading-catalog tools. The currently
                installed source is Toonverse, under the `open-ink-library.toonverse` tool family.

                ## Available Toonverse functions

                - `fetch` requires an exact normalized `name` or Toonverse slug and returns work metadata.
                - `fetch-full` accepts the same `name` plus optional `limit` (1–500, default 50),
                  `offset` (zero-based, default 0), and `order` (`asc` or `desc`, default `asc`) to
                  return a chapter page with the metadata.
                - `fetch-chapter` requires `name` and a zero-based `chapterNumber`.
                - `fetch-chapters` requires `name`, `minChapterNumber`, and `maxChapterNumber`; the
                  inclusive range is returned in ascending order.
                - `search` accepts optional name, genre, type, chapter-count, rating, author, status,
                  sort, limit, and offset filters. Its limit is 1–100 and defaults to 20.
                - `download-book` requires `name`, `minChapterNumber`, and `maxChapterNumber`; optional
                  `ttlSeconds` and `maxRequests` are bounded by the server storage policy. It writes a
                  temporary Meshingress workspace containing the selected chapter descriptors and pages.

                Exact work lookup never silently selects a fuzzy search result. Toonverse is currently
                configured as a public source: callers must not provide a bearer token, and this module
                neither accepts nor persists one.

                ## Source profile and deployment configuration

                This module declares no host-editable `application.properties` fields. The Toonverse
                adapter reads its bundled, validated `open-ink-library/toonverse.yaml` profile: HTTPS API
                base URL `https://api.toonverse.net`, a 20,000 ms source timeout, `authMode: none`, search
                defaults of 20/100, and chapter-page defaults of 50/500 with ascending order. Those are
                module-owned source-profile values, not runtime property overrides. Change them only by
                updating and validating the bundled source profile in a new module build.
                """);
    }
}
