package dev.mrk.meshingress.dispatch.data;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RecordContent extends StructuredContent {
    private String id;
    private String title;
    private JsonNode record;

    public RecordContent() {
        super(StructuredContentKind.Data.DATA_RECORD);
    }

}
