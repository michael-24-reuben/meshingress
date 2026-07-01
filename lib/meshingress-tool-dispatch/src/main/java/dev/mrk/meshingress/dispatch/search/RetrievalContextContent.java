package dev.mrk.meshingress.dispatch.search;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class RetrievalContextContent extends StructuredContent {
    @Setter
    private String query;
    @Setter
    private String answer;
    private List<SearchResult> contexts = new ArrayList<>();

    public RetrievalContextContent() {
        super(StructuredContentKind.Search.RETRIEVAL_CONTEXT);
    }

    public void setContexts(List<SearchResult> contexts) {
        this.contexts = contexts == null ? new ArrayList<>() : new ArrayList<>(contexts);
    }
}
