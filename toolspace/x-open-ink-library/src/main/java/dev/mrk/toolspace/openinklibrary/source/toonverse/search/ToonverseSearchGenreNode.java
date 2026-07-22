package dev.mrk.toolspace.openinklibrary.source.toonverse.search;

public class ToonverseSearchGenreNode {
    private String id;
    private String name;
    private String slug;

    public ToonverseSearchGenreNode(String id, String name, String slug) {
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
