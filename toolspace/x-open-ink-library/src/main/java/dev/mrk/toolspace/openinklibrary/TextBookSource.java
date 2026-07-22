package dev.mrk.toolspace.openinklibrary;

public interface TextBookSource extends BookSource {
    @Override
    default BookKind bookKind() {
        return BookKind.TEXT_BOOK;
    }
}
