package dev.mrk.meshingress.dispatch.tool;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ToolListContent extends StructuredContent {
    private List<ToolDescriptorContent> tools = new ArrayList<>();
    @Setter
    private Integer total;

    public ToolListContent() {
        super(StructuredContentKind.Tool.TOOL_LIST);
    }

    public void setTools(List<ToolDescriptorContent> tools) {
        this.tools = tools == null ? new ArrayList<>() : new ArrayList<>(tools);
    }

}
