package dev.mrk.toolspace.openinklibrary;

import java.util.List;

/** A source chapter index entry. Page content is deliberately a separate capability. */
public record ImageBookChapterMetadata(
        String sourceChapterId,
        Double number,
        String title,
        String publishedAt,
        String sourceUrl,
        Long reads,
        List<Page> pages
) {
    public ImageBookChapterMetadata {
        pages = List.copyOf(pages);
    }

    public record Page(int index, String url) {
    }
}
