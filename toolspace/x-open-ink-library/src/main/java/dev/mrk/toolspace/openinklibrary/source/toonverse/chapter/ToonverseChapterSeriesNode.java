package dev.mrk.toolspace.openinklibrary.source.toonverse.chapter;

import java.util.List;

public class ToonverseChapterSeriesNode {
    private String id;
    private String title;
    private String slug;
    private String coverUrl;
    private String type;
    private List<String> genres;

    public ToonverseChapterSeriesNode(String id, String title, String slug, String coverUrl, String type, List<String> genres) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.coverUrl = coverUrl;
        this.type = type;
        this.genres = genres;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSlug() {
        return this.slug;
    }

    public String getCoverUrl() {
        return this.coverUrl;
    }

    public String getType() {
        return this.type;
    }

    public List<String> getGenres() {
        return this.genres;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
}
