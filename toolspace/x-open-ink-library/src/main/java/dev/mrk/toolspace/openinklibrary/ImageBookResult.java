package dev.mrk.toolspace.openinklibrary;

import java.util.List;

/** Canonical complete image-book extraction result; it intentionally contains no credentials or raw API body. */
public record ImageBookResult(
        int schemaVersion,
        BookKind bookKind,
        Source source,
        Work work,
        List<ImageBookChapterMetadata> chapters,
        Metrics metrics
) {
    public ImageBookResult {
        chapters = List.copyOf(chapters);
    }

    public record Source(String id, String workId, String url) {
    }

    public record Work(
            String title,
            List<String> alternativeTitles,
            String description,
            List<String> authors,
            List<String> artists,
            String publisher,
            String publicationYear,
            PublicationType publicationType,
            PublicationStatus status,
            List<String> genres,
            List<Cover> covers
    ) {
        public Work {
            alternativeTitles = List.copyOf(alternativeTitles);
            authors = List.copyOf(authors);
            artists = List.copyOf(artists);
            genres = List.copyOf(genres);
            covers = List.copyOf(covers);
        }
    }

    public record Cover(String url) {
    }

    public record Metrics(Integer chapterCount) {
    }
}
