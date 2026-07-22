package dev.mrk.toolspace.openinklibrary.source.toonverse.chapter;

import java.util.List;

public class ToonverseChapterNode {
    private String id;
    private String seriesId;
    private Long number;
    private String title;
    private String publishedAt;
    private List<ToonverseChapterPageNode> pages;

    public ToonverseChapterNode(String id, String seriesId, Long number, String title, String publishedAt, List<ToonverseChapterPageNode> pages) {
        this.id = id;
        this.seriesId = seriesId;
        this.number = number;
        this.title = title;
        this.publishedAt = publishedAt;
        this.pages = pages;
    }

    public String getId() {
        return this.id;
    }

    public String getSeriesId() {
        return this.seriesId;
    }

    public Long getNumber() {
        return this.number;
    }

    public String getTitle() {
        return this.title;
    }

    public String getPublishedAt() {
        return this.publishedAt;
    }

    public List<ToonverseChapterPageNode> getPages() {
        return this.pages;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setPages(List<ToonverseChapterPageNode> pages) {
        this.pages = pages;
    }
}
