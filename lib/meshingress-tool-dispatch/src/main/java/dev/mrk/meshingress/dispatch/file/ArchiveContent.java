package dev.mrk.meshingress.dispatch.file;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ArchiveContent extends StructuredContent {
    @Setter
    private String name;
    @Setter
    private String path;
    @Setter
    private String url;
    @Setter
    private String mimeType;
    @Setter
    private Long sizeBytes;
    @Setter
    private String format;
    private List<FileEntry> entries = new ArrayList<>();

    public ArchiveContent() {
        super(StructuredContentKind.File.FILE_ARCHIVE);
    }

    public void setEntries(List<FileEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }
}
