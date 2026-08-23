package dev.mrk.toolspace.youtube.catalog;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;

import java.util.List;

/** Structured payload for the YouTube provider catalog. */
public final class YoutubeProvidersContent extends StructuredContent {
    private final List<YoutubeProvider> records;
    private final int total;

    public YoutubeProvidersContent(List<YoutubeProvider> records) {
        super(StructuredContentKind.Data.DATA_RECORDS);
        this.records = List.copyOf(records);
        this.total = this.records.size();
    }

    public List<YoutubeProvider> getRecords() {
        return records;
    }

    public int getTotal() {
        return total;
    }
}
