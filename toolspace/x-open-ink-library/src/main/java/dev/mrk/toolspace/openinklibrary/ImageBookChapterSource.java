package dev.mrk.toolspace.openinklibrary;

import java.util.List;

public interface ImageBookChapterSource extends ImageBookSource {
    List<ImageBookChapterMetadata> fetchChapters(ImageBookChapterListRequest request);
}
