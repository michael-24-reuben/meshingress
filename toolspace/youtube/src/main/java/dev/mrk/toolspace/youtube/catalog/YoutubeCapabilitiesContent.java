package dev.mrk.toolspace.youtube.catalog;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;

import java.util.List;

/** Structured payload for the YouTube capability catalog. */
public final class YoutubeCapabilitiesContent extends StructuredContent {
    private final List<YoutubeCapability> records;
    private final int total;

    public YoutubeCapabilitiesContent(List<YoutubeCapability> records) {
        super(StructuredContentKind.Data.DATA_RECORDS);
        this.records = List.copyOf(records);
        this.total = this.records.size();
    }

    public List<YoutubeCapability> getRecords() {
        return records;
    }

    public int getTotal() {
        return total;
    }
}
