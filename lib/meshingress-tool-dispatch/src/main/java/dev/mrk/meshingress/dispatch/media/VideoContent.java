package dev.mrk.meshingress.dispatch.media;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class VideoContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private Long durationMs;
    @Setter
    private Integer width;
    @Setter
    private Integer height;
    private List<MediaSource> sources = new ArrayList<>();
    @Setter
    private ImageRef poster;
    private List<ImageRef> thumbnails = new ArrayList<>();
    private List<CaptionTrack> captions = new ArrayList<>();
    @Setter
    private PersonRef author;
    @Setter
    private OriginRef origin;
    @Setter
    private MediaStats stats;

    public VideoContent() {
        super(StructuredContentKind.Media.MEDIA_VIDEO);
    }

    public void setSources(List<MediaSource> sources) {
        this.sources = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
    }

    public void setThumbnails(List<ImageRef> thumbnails) {
        this.thumbnails = thumbnails == null ? new ArrayList<>() : new ArrayList<>(thumbnails);
    }

    public void setCaptions(List<CaptionTrack> captions) {
        this.captions = captions == null ? new ArrayList<>() : new ArrayList<>(captions);
    }

}
