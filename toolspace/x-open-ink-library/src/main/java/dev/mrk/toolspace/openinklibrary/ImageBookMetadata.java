package dev.mrk.toolspace.openinklibrary;

import java.util.List;

public record ImageBookMetadata(
        String sourceId,
        String sourceWorkId,
        String sourceUrl,
        String title,
        List<String> alternativeTitles,
        String synopsis,
        List<String> authors,
        List<String> artists,
        String publisher,
        String publicationYear,
        PublicationType publicationType,
        PublicationStatus publicationStatus,
        List<String> genres,
        String coverUrl,
        Integer chapterCount
) {
    public ImageBookMetadata {
        alternativeTitles = List.copyOf(alternativeTitles);
        authors = List.copyOf(authors);
        artists = List.copyOf(artists);
        genres = List.copyOf(genres);
    }
}
