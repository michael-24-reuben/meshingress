package dev.mrk.meshingress.dispatch.file;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class DirectoryContent extends StructuredContent {
    @Setter
    private String name;
    @Setter
    private String path;
    @Setter
    private String url;
    private List<FileEntry> entries = new ArrayList<>();

    public DirectoryContent() {
        super(StructuredContentKind.File.FILE_DIRECTORY);
    }

    public void setEntries(List<FileEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }
}
