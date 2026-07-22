package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

import java.util.List;

public record ToonverseSearchArgs(
        @McpInputField(value = "name", description = "Optional work name to search.", required = false) String name,
        @McpInputField(value = "genres", description = "Genres to include; use genreMode to combine them.", required = false) List<String> genres,
        @McpInputField(value = "excludeGenres", description = "Genres to exclude.", required = false) List<String> excludeGenres,
        @McpInputField(value = "genreMode", description = "Genre operator: or or and.", required = false) String genreMode,
        @McpInputField(value = "type", description = "Publication type: manhwa, manhua, or manga.", required = false) String type,
        @McpInputField(value = "minChapters", description = "Minimum chapter count.", required = false) Integer minChapters,
        @McpInputField(value = "maxChapters", description = "Maximum chapter count.", required = false) Integer maxChapters,
        @McpInputField(value = "minRating", description = "Minimum rating from 0 through 5.", required = false) Double minRating,
        @McpInputField(value = "maxRating", description = "Maximum rating from 0 through 5.", required = false) Double maxRating,
        @McpInputField(value = "author", description = "Author name filter.", required = false) String author,
        @McpInputField(value = "status", description = "Publication status: ongoing, completed, or hiatus.", required = false) String status,
        @McpInputField(value = "sortBy", description = "Sort: popular, trending, updated, rating, library, newest, chapters, or alphabetical.", required = false) String sortBy,
        @McpInputField(value = "limit", description = "Result count from 1 through 100; defaults to 20.", required = false) Integer limit,
        @McpInputField(value = "offset", description = "Zero-based result offset.", required = false) Integer offset
) {
    ToonverseSearchRequest toRequest() {
        return new ToonverseSearchRequest(name, genres, excludeGenres, genreMode, type, minChapters, maxChapters,
                minRating, maxRating, author, status, sortBy, limit, offset);
    }
}
