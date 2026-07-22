package dev.mrk.toolspace.openinklibrary;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookSourceRegistryTests {

    @Test
    void normalizesIdsAndListsDescriptors() {
        ImageBookMetadataSource source = new StubSource("ToOnVeRsE");
        BookSourceRegistry<ImageBookSource> registry = new BookSourceRegistry<>(List.of(source));

        assertEquals(source, registry.require(" toonverse "));
        assertEquals("toonverse", registry.list().getFirst().id());
        assertEquals(source, registry.requireCapability("TOONVERSE", ImageBookMetadataSource.class));
    }

    @Test
    void rejectsDuplicateNormalizedIds() {
        assertThrows(IllegalStateException.class, () -> new BookSourceRegistry<>(List.of(
                new StubSource("toonverse"),
                new StubSource("TOONVERSE")
        )));
    }

    @Test
    void reportsUnknownSourcesAndMissingCapabilities() {
        BookSourceRegistry<ImageBookSource> registry = new BookSourceRegistry<>(List.of(new StubSource("toonverse")));

        assertEquals("UNKNOWN_BOOK_SOURCE", assertThrows(SourceException.class, () -> registry.require("missing")).code());
        assertEquals("UNSUPPORTED_SOURCE_CAPABILITY", assertThrows(
                SourceException.class,
                () -> registry.requireCapability("toonverse", Runnable.class)
        ).code());
    }

    private record StubSource(String sourceId) implements ImageBookMetadataSource {
        @Override
        public String displayName() {
            return "Stub";
        }

        @Override
        public BookSourceDescriptor descriptor() {
            return new BookSourceDescriptor(sourceId(), displayName(), bookKind(), Set.of("metadata"));
        }

        @Override
        public ImageBookMetadata fetchMetadata(ImageBookMetadataRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
