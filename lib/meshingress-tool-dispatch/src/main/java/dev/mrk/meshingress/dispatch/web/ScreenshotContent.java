package dev.mrk.meshingress.dispatch.web;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ScreenshotContent extends StructuredContent {
    private String url;
    private String imageUrl;
    private String path;
    private String mimeType;
    private Integer width;
    private Integer height;

    public ScreenshotContent() {
        super(StructuredContentKind.Web.WEB_SCREENSHOT);
    }

}
