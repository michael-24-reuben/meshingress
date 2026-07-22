package dev.mrk.toolspace.openinklibrary.source.toonverse.series;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ToonverseSeriesDataNode {
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
    private Boolean featured;
    private Boolean trending;
    private String createdAt;
    private String updatedAt;
    private List<String> alternativeNames;
    private String artist;
    private String publisher;
    private String year;
    private String approvalStatus;
    private Boolean approved;
    private String rejectionReason;
    private String reviewedAt;
    private String reviewedById;
    private String submittedById;
    private Long chapterCount;
    private Boolean isOriginal;
    private Boolean isAdult;
    private String lastChapterAt;
    private List<ToonverseSeriesGenreNode> genres;
    @JsonProperty("_count")
    private ToonverseSeriesCountNode count;
    private Long firstChapterNumber;
    private Long latestChapterNumber;
    private String description;
    private Long viewCount;
    private Long chapterReads;
    private Boolean isPromoted;

    public ToonverseSeriesDataNode(String id, String title, String slug, String coverUrl, String author, String synopsis, String type, String status, Long rating, Long ratingCount, Boolean featured, Boolean trending, String createdAt, String updatedAt, List<String> alternativeNames, String artist, String publisher, String year, String approvalStatus, Boolean approved, String rejectionReason, String reviewedAt, String reviewedById, String submittedById, Long chapterCount, Boolean isOriginal, Boolean isAdult, String lastChapterAt, List<ToonverseSeriesGenreNode> genres, ToonverseSeriesCountNode count, Long firstChapterNumber, Long latestChapterNumber, String description, Long viewCount, Long chapterReads, Boolean isPromoted) {
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
        this.featured = featured;
        this.trending = trending;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.alternativeNames = alternativeNames;
        this.artist = artist;
        this.publisher = publisher;
        this.year = year;
        this.approvalStatus = approvalStatus;
        this.approved = approved;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = reviewedAt;
        this.reviewedById = reviewedById;
        this.submittedById = submittedById;
        this.chapterCount = chapterCount;
        this.isOriginal = isOriginal;
        this.isAdult = isAdult;
        this.lastChapterAt = lastChapterAt;
        this.genres = genres;
        this.count = count;
        this.firstChapterNumber = firstChapterNumber;
        this.latestChapterNumber = latestChapterNumber;
        this.description = description;
        this.viewCount = viewCount;
        this.chapterReads = chapterReads;
        this.isPromoted = isPromoted;
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

    public Boolean getFeatured() {
        return this.featured;
    }

    public Boolean getTrending() {
        return this.trending;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public List<String> getAlternativeNames() {
        return this.alternativeNames;
    }

    public String getArtist() {
        return this.artist;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public String getYear() {
        return this.year;
    }

    public String getApprovalStatus() {
        return this.approvalStatus;
    }

    public Boolean getApproved() {
        return this.approved;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public String getReviewedAt() {
        return this.reviewedAt;
    }

    public String getReviewedById() {
        return this.reviewedById;
    }

    public String getSubmittedById() {
        return this.submittedById;
    }

    public Long getChapterCount() {
        return this.chapterCount;
    }

    public Boolean getIsOriginal() {
        return this.isOriginal;
    }

    public Boolean getIsAdult() {
        return this.isAdult;
    }

    public String getLastChapterAt() {
        return this.lastChapterAt;
    }

    public List<ToonverseSeriesGenreNode> getGenres() {
        return this.genres;
    }

    public ToonverseSeriesCountNode getCount() {
        return this.count;
    }

    public Long getFirstChapterNumber() {
        return this.firstChapterNumber;
    }

    public Long getLatestChapterNumber() {
        return this.latestChapterNumber;
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

    public Boolean getIsPromoted() {
        return this.isPromoted;
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

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public void setTrending(Boolean trending) {
        this.trending = trending;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public void setReviewedById(String reviewedById) {
        this.reviewedById = reviewedById;
    }

    public void setSubmittedById(String submittedById) {
        this.submittedById = submittedById;
    }

    public void setChapterCount(Long chapterCount) {
        this.chapterCount = chapterCount;
    }

    public void setIsOriginal(Boolean isOriginal) {
        this.isOriginal = isOriginal;
    }

    public void setIsAdult(Boolean isAdult) {
        this.isAdult = isAdult;
    }

    public void setLastChapterAt(String lastChapterAt) {
        this.lastChapterAt = lastChapterAt;
    }

    public void setGenres(List<ToonverseSeriesGenreNode> genres) {
        this.genres = genres;
    }

    public void setCount(ToonverseSeriesCountNode count) {
        this.count = count;
    }

    public void setFirstChapterNumber(Long firstChapterNumber) {
        this.firstChapterNumber = firstChapterNumber;
    }

    public void setLatestChapterNumber(Long latestChapterNumber) {
        this.latestChapterNumber = latestChapterNumber;
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

    public void setIsPromoted(Boolean isPromoted) {
        this.isPromoted = isPromoted;
    }
}
