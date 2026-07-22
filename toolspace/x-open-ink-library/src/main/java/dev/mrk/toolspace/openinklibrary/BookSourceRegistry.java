package dev.mrk.toolspace.openinklibrary;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BookSourceRegistry<S extends BookSource> {
    private final Map<String, S> sources;

    public BookSourceRegistry(Collection<S> configuredSources) {
        Map<String, S> indexed = new LinkedHashMap<>();
        for (S source : configuredSources) {
            String id = Utility.normalize(source.sourceId());
            if (indexed.putIfAbsent(id, source) != null) {
                throw new IllegalStateException("Duplicate normalized book source ID: " + id);
            }
        }
        this.sources = Map.copyOf(indexed);
    }

    public List<BookSourceDescriptor> list() {
        return sources.values().stream()
                .map(source -> normalizedDescriptor(source.descriptor()))
                .sorted(Comparator.comparing(BookSourceDescriptor::id))
                .toList();
    }

    public S require(String sourceId) {
        S source = sources.get(Utility.normalize(sourceId));
        if (source == null) {
            throw new SourceException("UNKNOWN_BOOK_SOURCE", "No installed source matches '" + sourceId + "'.");
        }
        return source;
    }

    public <C> C requireCapability(String sourceId, Class<C> capability) {
        S source = require(sourceId);
        if (!capability.isInstance(source)) {
            throw new SourceException(
                    "UNSUPPORTED_SOURCE_CAPABILITY",
                    "Source '" + source.sourceId() + "' does not support " + capability.getSimpleName() + "."
            );
        }
        return capability.cast(source);
    }


    private static BookSourceDescriptor normalizedDescriptor(BookSourceDescriptor descriptor) {
        return new BookSourceDescriptor(
                Utility.normalize(descriptor.id()),
                descriptor.displayName(),
                descriptor.bookKind(),
                descriptor.capabilities()
        );
    }
}
