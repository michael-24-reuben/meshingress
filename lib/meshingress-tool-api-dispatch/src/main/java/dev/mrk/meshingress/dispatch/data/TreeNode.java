package dev.mrk.meshingress.dispatch.data;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class TreeNode {
    private String id;
    private String label;
    private String type;
    private JsonNode value;
    private List<TreeNode> children = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public JsonNode getValue() { return value; }
    public void setValue(JsonNode value) { this.value = value; }
    public List<TreeNode> getChildren() { return children; }
    public void setChildren(List<TreeNode> children) { this.children = children == null ? new ArrayList<>() : new ArrayList<>(children); }
}
