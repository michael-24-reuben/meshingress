package dev.mrk.meshingress.dispatch.tool;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ToolDescriptorContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private String defaultFunction;
    private List<String> scopes = new ArrayList<>();
    @Setter
    private JsonNode inputSchema;

    public ToolDescriptorContent() {
        super(StructuredContentKind.Tool.TOOL_DESCRIPTOR);
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
    }

}
