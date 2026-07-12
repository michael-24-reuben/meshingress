package dev.mrk.meshingress.dispatch.data;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class TableContent extends StructuredContent {
    @Setter
    private String title;
    private List<TableColumn> columns = new ArrayList<>();
    private List<JsonNode> rows = new ArrayList<>();
    @Setter
    private Integer total;

    public TableContent() {
        super(StructuredContentKind.Data.DATA_TABLE);
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
    }

    public void setRows(List<JsonNode> rows) {
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

}
