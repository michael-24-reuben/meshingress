package dev.mrk.meshingress.dispatch.web;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class WebPageContent extends StructuredContent {
    @Setter
    private String url;
    @Setter
    private String finalUrl;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private String html;
    @Setter
    private String text;
    private List<WebLinkContent> links = new ArrayList<>();

    public WebPageContent() {
        super(StructuredContentKind.Web.WEB_PAGE);
    }

    public void setLinks(List<WebLinkContent> links) {
        this.links = links == null ? new ArrayList<>() : new ArrayList<>(links);
    }
}
