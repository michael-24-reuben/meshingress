package dev.mrk.meshingress.dispatch.media;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class TranscriptContent extends StructuredContent {
    @Setter
    private String language;
    @Setter
    private String text;
    private List<TranscriptSegment> segments = new ArrayList<>();
    @Setter
    private OriginRef origin;

    public TranscriptContent() {
        super(StructuredContentKind.Media.MEDIA_TRANSCRIPT);
    }

    public void setSegments(List<TranscriptSegment> segments) {
        this.segments = segments == null ? new ArrayList<>() : new ArrayList<>(segments);
    }

}
