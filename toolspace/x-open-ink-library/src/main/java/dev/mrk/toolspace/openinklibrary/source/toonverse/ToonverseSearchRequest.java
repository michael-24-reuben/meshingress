package dev.mrk.toolspace.openinklibrary.source.toonverse;

import java.util.List;

public record ToonverseSearchRequest(
        String name,
        List<String> genres,
        List<String> excludeGenres,
        String genreMode,
        String type,
        Integer minChapters,
        Integer maxChapters,
        Double minRating,
        Double maxRating,
        String author,
        String status,
        String sortBy,
        Integer limit,
        Integer offset
) {
    public ToonverseSearchRequest {
        genres = genres == null ? List.of() : List.copyOf(genres);
        excludeGenres = excludeGenres == null ? List.of() : List.copyOf(excludeGenres);
    }
}
