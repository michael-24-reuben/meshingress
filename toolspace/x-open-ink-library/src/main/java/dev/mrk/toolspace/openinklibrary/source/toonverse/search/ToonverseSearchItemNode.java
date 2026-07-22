package dev.mrk.toolspace.openinklibrary.source.toonverse.search;

import java.util.List;

public class ToonverseSearchItemNode {
    private String id;
    private String title;
    private String slug;
    private String coverUrl;
    private String author;
    private String synopsis;
    private String type;
    private String status;
    private Long rating;
    private Long ratingCount;
    private Long chapterCount;
    private Boolean featured;
    private Boolean trending;
    private Boolean isOriginal;
    private Boolean isAdult;
    private String createdAt;
    private String updatedAt;
    private List<ToonverseSearchGenreNode> genres;
    private String description;
    private Long viewCount;
    private Long chapterReads;

    public ToonverseSearchItemNode(String id, String title, String slug, String coverUrl, String author, String synopsis, String type, String status, Long rating, Long ratingCount, Long chapterCount, Boolean featured, Boolean trending, Boolean isOriginal, Boolean isAdult, String createdAt, String updatedAt, List<ToonverseSearchGenreNode> genres, String description, Long viewCount, Long chapterReads) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.coverUrl = coverUrl;
        this.author = author;
        this.synopsis = synopsis;
        this.type = type;
        this.status = status;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.chapterCount = chapterCount;
        this.featured = featured;
        this.trending = trending;
        this.isOriginal = isOriginal;
        this.isAdult = isAdult;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.genres = genres;
        this.description = description;
        this.viewCount = viewCount;
        this.chapterReads = chapterReads;
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

    public String getAuthor() {
        return this.author;
    }

    public String getSynopsis() {
        return this.synopsis;
    }

    public String getType() {
        return this.type;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getRating() {
        return this.rating;
    }

    public Long getRatingCount() {
        return this.ratingCount;
    }

    public Long getChapterCount() {
        return this.chapterCount;
    }

    public Boolean getFeatured() {
        return this.featured;
    }

    public Boolean getTrending() {
        return this.trending;
    }

    public Boolean getIsOriginal() {
        return this.isOriginal;
    }

    public Boolean getIsAdult() {
        return this.isAdult;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public List<ToonverseSearchGenreNode> getGenres() {
        return this.genres;
    }

    public String getDescription() {
        return this.description;
    }

    public Long getViewCount() {
        return this.viewCount;
    }

    public Long getChapterReads() {
        return this.chapterReads;
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

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRating(Long rating) {
        this.rating = rating;
    }

    public void setRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public void setChapterCount(Long chapterCount) {
        this.chapterCount = chapterCount;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public void setTrending(Boolean trending) {
        this.trending = trending;
    }

    public void setIsOriginal(Boolean isOriginal) {
        this.isOriginal = isOriginal;
    }

    public void setIsAdult(Boolean isAdult) {
        this.isAdult = isAdult;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setGenres(List<ToonverseSearchGenreNode> genres) {
        this.genres = genres;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setChapterReads(Long chapterReads) {
        this.chapterReads = chapterReads;
    }
}
