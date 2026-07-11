package dev.mrk.meshingress.dispatch.web;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class WebElementContent extends StructuredContent {
    @Setter
    private String tagName;
    @Setter
    private String text;
    @Setter
    private String selector;
    @Setter
    private String xpath;
    private Map<String, String> attributes = new LinkedHashMap<>();
    @Setter
    private Double score;

    public WebElementContent() {
        super(StructuredContentKind.Web.WEB_ELEMENT);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

}
