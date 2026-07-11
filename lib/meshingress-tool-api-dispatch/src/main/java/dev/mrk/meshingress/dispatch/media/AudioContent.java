package dev.mrk.meshingress.dispatch.media;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class AudioContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private Long durationMs;
    private List<MediaSource> sources = new ArrayList<>();
    @Setter
    private ImageRef artwork;
    @Setter
    private PersonRef author;
    @Setter
    private OriginRef origin;

    public AudioContent() {
        super(StructuredContentKind.Media.MEDIA_AUDIO);
    }

    public void setSources(List<MediaSource> sources) {
        this.sources = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
    }

}
