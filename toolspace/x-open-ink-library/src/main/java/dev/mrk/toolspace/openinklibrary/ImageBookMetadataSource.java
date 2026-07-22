package dev.mrk.toolspace.openinklibrary;

public interface ImageBookMetadataSource extends ImageBookSource {
    ImageBookMetadata fetchMetadata(ImageBookMetadataRequest request);
}
