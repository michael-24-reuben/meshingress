package dev.mrk.meshingress.dispatch.file;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class FileManifestContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String basePath;
    private List<FileEntry> files = new ArrayList<>();

    public FileManifestContent() {
        super(StructuredContentKind.File.FILE_MANIFEST);
    }

    public void setFiles(List<FileEntry> files) {
        this.files = files == null ? new ArrayList<>() : new ArrayList<>(files);
    }
}
