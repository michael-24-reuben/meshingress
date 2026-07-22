package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.toolspace.openinklibrary.*;

import java.util.Set;

public final class ToonverseImageBookSource implements ImageBookMetadataSource, ImageBookChapterSource {
    private final ToonverseClient client;
    private final ToonverseMapper mapper;
    private final ToonverseSourceConfiguration configuration;

    public ToonverseImageBookSource(
            ToonverseClient client,
            ToonverseMapper mapper,
            ToonverseSourceConfiguration configuration
    ) {
        this.client = client;
        this.mapper = mapper;
        this.configuration = configuration;
    }

    @Override
    public String sourceId() {
        return configuration.id();
    }

    @Override
    public String displayName() {
        return configuration.displayName();
    }

    @Override
    public BookSourceDescriptor descriptor() {
        return new BookSourceDescriptor(sourceId(), displayName(), bookKind(), Set.of("metadata", "chapters", "extract"));
    }

    @Override
    public ImageBookMetadata fetchMetadata(ImageBookMetadataRequest request) {
        return mapper.toMetadata(client.fetchSeriesMetadata(request.slug(), request.authorizationToken()));
    }

    @Override
    public java.util.List<ImageBookChapterMetadata> fetchChapters(ImageBookChapterListRequest request) {
        return mapper.toChapters(client.fetchSeriesChapters(
                request.sourceWorkId(), request.limit(), request.order(), request.authorizationToken()
        ));
    }
}
