package dev.mrk.meshingress.dispatch.web;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class WebHtmlContent extends StructuredContent {
    private String url;
    private String html;
    private String text;
    private boolean sanitized;

    public WebHtmlContent() {
        super(StructuredContentKind.Web.WEB_HTML);
    }

}
