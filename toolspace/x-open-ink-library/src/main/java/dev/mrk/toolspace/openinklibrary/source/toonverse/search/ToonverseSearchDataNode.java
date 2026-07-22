package dev.mrk.toolspace.openinklibrary.source.toonverse.search;

import java.util.List;

public class ToonverseSearchDataNode {
    private List<ToonverseSearchItemNode> items;
    private Long total;
    private Long limit;
    private Long offset;
    private Boolean hasMore;

    public ToonverseSearchDataNode(List<ToonverseSearchItemNode> items, Long total, Long limit, Long offset, Boolean hasMore) {
        this.items = items;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.hasMore = hasMore;
    }

    public List<ToonverseSearchItemNode> getItems() {
        return this.items;
    }

    public Long getTotal() {
        return this.total;
    }

    public Long getLimit() {
        return this.limit;
    }

    public Long getOffset() {
        return this.offset;
    }

    public Boolean getHasMore() {
        return this.hasMore;
    }

    public void setItems(List<ToonverseSearchItemNode> items) {
        this.items = items;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}
