package dev.mrk.meshingress.dispatch.media;

public record MediaSource(
        String url,
        String mimeType,
        String quality,
        Integer width,
        Integer height,
        Long bitrate,
        Long sizeBytes
) {
    public MediaSource(String url, String mimeType, String quality) {
        this(url, mimeType, quality, null, null, null, null);
    }
}
