package dev.mrk.meshingress.dispatch.text;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class MarkdownContent extends StructuredContent {
    private String title;
    private String markdown;
    private String language;

    public MarkdownContent() {
        super(StructuredContentKind.Text.TEXT_MARKDOWN);
    }

    public MarkdownContent(String markdown) {
        this();
        this.markdown = markdown;
    }

}
