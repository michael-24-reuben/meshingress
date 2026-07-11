package dev.mrk.meshingress.dispatch.web;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class WebLinkContent extends StructuredContent {
    private String url;
    private String title;
    private String description;
    private String imageUrl;
    private String siteName;

    public WebLinkContent() {
        super(StructuredContentKind.Web.WEB_LINK);
    }

}
