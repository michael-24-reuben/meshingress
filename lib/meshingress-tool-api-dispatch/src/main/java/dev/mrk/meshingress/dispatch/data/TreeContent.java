package dev.mrk.meshingress.dispatch.data;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class TreeContent extends StructuredContent {
    private String title;
    private TreeNode root;

    public TreeContent() {
        super(StructuredContentKind.Data.DATA_TREE);
    }

}
