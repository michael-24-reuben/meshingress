package dev.mrk.meshingress.dispatch.search;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class SearchResultsContent extends StructuredContent {
    @Setter
    private String query;
    private List<SearchResult> results = new ArrayList<>();
    @Setter
    private Integer total;

    public SearchResultsContent() {
        super(StructuredContentKind.Search.SEARCH_RESULTS);
    }

    public void setResults(List<SearchResult> results) {
        this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
    }

}
