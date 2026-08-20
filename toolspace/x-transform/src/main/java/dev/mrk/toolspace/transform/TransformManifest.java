package dev.mrk.toolspace.transform;

import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolModuleMetadata;

import java.util.List;

/** Module identity shared by Transform tool-family handlers. */
public final class TransformManifest implements McpToolManifestDefinition {
    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "transform",
                "Transform",
                "Source-code transformations",
                "Backend-only polyglot transformations from ritz078/transform.",
                List.of(), "", List.of("code", "transform"), List.of(), null
        );
    }
}
