package dev.mrk.meshingress.dispatch.media;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class GalleryContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String description;
    private List<MediaItemRef> items = new ArrayList<>();
    @Setter
    private OriginRef origin;

    public GalleryContent() {
        super(StructuredContentKind.Media.MEDIA_GALLERY);
    }

    public void setItems(List<MediaItemRef> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

}
