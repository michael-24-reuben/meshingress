package dev.mrk.toolspace.openinklibrary.source.toonverse.chapter;

import java.util.List;

public class ToonverseChapterDataNode {
    private ToonverseChapterNode chapter;
    private ToonverseChapterSeriesNode series;
    private Long totalChapters;
    private List<Double> chapterNumbers;
    private Boolean hasPrevious;
    private Boolean hasNext;
    private Long previousChapter;
    private Long nextChapter;

    public ToonverseChapterDataNode(ToonverseChapterNode chapter, ToonverseChapterSeriesNode series, Long totalChapters, List<Double> chapterNumbers, Boolean hasPrevious, Boolean hasNext, Long previousChapter, Long nextChapter) {
        this.chapter = chapter;
        this.series = series;
        this.totalChapters = totalChapters;
        this.chapterNumbers = chapterNumbers;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
        this.previousChapter = previousChapter;
        this.nextChapter = nextChapter;
    }

    public ToonverseChapterNode getChapter() {
        return this.chapter;
    }

    public ToonverseChapterSeriesNode getSeries() {
        return this.series;
    }

    public Long getTotalChapters() {
        return this.totalChapters;
    }

    public List<Double> getChapterNumbers() {
        return this.chapterNumbers;
    }

    public Boolean getHasPrevious() {
        return this.hasPrevious;
    }

    public Boolean getHasNext() {
        return this.hasNext;
    }

    public Long getPreviousChapter() {
        return this.previousChapter;
    }

    public Long getNextChapter() {
        return this.nextChapter;
    }

    public void setChapter(ToonverseChapterNode chapter) {
        this.chapter = chapter;
    }

    public void setSeries(ToonverseChapterSeriesNode series) {
        this.series = series;
    }

    public void setTotalChapters(Long totalChapters) {
        this.totalChapters = totalChapters;
    }

    public void setChapterNumbers(List<Double> chapterNumbers) {
        this.chapterNumbers = chapterNumbers;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public void setPreviousChapter(Long previousChapter) {
        this.previousChapter = previousChapter;
    }

    public void setNextChapter(Long nextChapter) {
        this.nextChapter = nextChapter;
    }
}
