package dev.mrk.meshingress.dispatch.file;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class FileContent extends StructuredContent {
    private String name;
    private String path;
    private String url;
    private String mimeType;
    private Long sizeBytes;
    private String checksum;
    private String checksumAlgorithm;

    public FileContent() {
        super(StructuredContentKind.File.FILE_GENERIC);
    }

}
