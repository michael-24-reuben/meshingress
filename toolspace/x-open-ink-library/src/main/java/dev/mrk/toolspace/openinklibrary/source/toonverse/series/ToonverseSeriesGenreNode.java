package dev.mrk.toolspace.openinklibrary.source.toonverse.series;

public class ToonverseSeriesGenreNode {
    private String id;
    private String name;
    private String slug;

    public ToonverseSeriesGenreNode(String id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getSlug() {
        return this.slug;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
