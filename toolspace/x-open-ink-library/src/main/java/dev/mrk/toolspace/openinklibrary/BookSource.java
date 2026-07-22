package dev.mrk.toolspace.openinklibrary;

public interface BookSource {
    String sourceId();

    String displayName();

    BookKind bookKind();

    BookSourceDescriptor descriptor();
}
