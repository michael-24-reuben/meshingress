package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolIcon;
import dev.mrk.meshingress.toolmetadata.ToolModuleMetadata;
import dev.mrk.meshingress.toolmetadata.ToolReadme;

import java.util.List;

/**
 * Module identity shared by every Hello World tool-family handler.
 */
public final class HelloWorldManifest implements McpToolManifestDefinition {
    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "helloworld",
                "Hello World",
                "Sample greeting tools",
                "Sample tools used to demonstrate Meshingress module and input-schema behavior.",
                List.of(), "", List.of("sample", "greeting"), List.of(),
                new ToolIcon("helloworld.svg", ToolIcon.MimeType.SVG_IMAGE, "Hello World")
        );
    }

    @Override
    public ToolReadme readme() {
        return ToolReadme.inline("""
                # Hello World
                
                Exposes the `helloworld.greeting.greet` sample MCP function.
                """);
    }
}
