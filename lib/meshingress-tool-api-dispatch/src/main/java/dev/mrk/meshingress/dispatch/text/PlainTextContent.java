package dev.mrk.meshingress.dispatch.text;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class PlainTextContent extends StructuredContent {
    private String title;
    private String text;
    private String language;

    public PlainTextContent() {
        super(StructuredContentKind.Text.TEXT_PLAIN);
    }

    public PlainTextContent(String text) {
        this();
        this.text = text;
    }

}
