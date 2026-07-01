package dev.mrk.meshingress.dispatch.media;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ImageContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private String url;
    @Setter
    private String mimeType;
    @Setter
    private Integer width;
    @Setter
    private Integer height;
    @Setter
    private String alt;
    private List<ImageRef> variants = new ArrayList<>();
    @Setter
    private PersonRef author;
    @Setter
    private OriginRef origin;

    public ImageContent() {
        super(StructuredContentKind.Media.MEDIA_IMAGE);
    }

    public void setVariants(List<ImageRef> variants) {
        this.variants = variants == null ? new ArrayList<>() : new ArrayList<>(variants);
    }

}
