package dev.mrk.meshingress.dispatch.text;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

/** Structured source-code output with a language hint for renderers. */
@Getter
@Setter
public final class CodeContent extends StructuredContent {
    private String title;
    private String code;
    private String language;

    public CodeContent() {
        super(StructuredContentKind.Text.TEXT_CODE);
    }

    public CodeContent(String code, String language) {
        this();
        this.code = code;
        this.language = language;
    }
}
