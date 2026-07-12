package dev.mrk.meshingress.dispatch.data;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class RecordsContent extends StructuredContent {
    @Setter
    private String title;
    private List<JsonNode> records = new ArrayList<>();
    @Setter
    private Integer total;

    public RecordsContent() {
        super(StructuredContentKind.Data.DATA_RECORDS);
    }

    public void setRecords(List<JsonNode> records) {
        this.records = records == null ? new ArrayList<>() : new ArrayList<>(records);
    }

}
