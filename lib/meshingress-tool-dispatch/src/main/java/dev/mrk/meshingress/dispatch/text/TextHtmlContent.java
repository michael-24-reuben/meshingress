package dev.mrk.meshingress.dispatch.text;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class TextHtmlContent extends StructuredContent {
    private String title;
    private String html;
    private String text;
    private boolean sanitized;

    public TextHtmlContent() {
        super(StructuredContentKind.Text.TEXT_HTML);
    }

}
