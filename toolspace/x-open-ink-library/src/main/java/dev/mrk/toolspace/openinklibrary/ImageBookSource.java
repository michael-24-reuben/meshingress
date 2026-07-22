package dev.mrk.toolspace.openinklibrary;

public interface ImageBookSource extends BookSource {
    @Override
    default BookKind bookKind() {
        return BookKind.IMAGE_BOOK;
    }
}
