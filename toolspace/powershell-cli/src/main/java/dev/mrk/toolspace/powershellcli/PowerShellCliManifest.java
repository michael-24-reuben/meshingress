package dev.mrk.toolspace.powershellcli;

import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.toolmetadata.*;

import java.util.List;

public final class PowerShellCliManifest implements McpToolManifestDefinition {

    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "powershell",
                "PowerShell CLI",
                "PowerShell command execution",
                "Executes PowerShell scripts through the PowerShell tool functions.",
                List.of(new ToolAuthor("Markus Ressel", new String[] { "mailto:markus@ressel.dev" })),
                "",
                List.of("powershell", "cli"),
                List.of(
                        ToolLink.documentation("https://learn.microsoft.com/powershell/").label("PowerShell Documentation"),
                        ToolLink.source("https://github.com/PowerShell/PowerShell").label("PowerShell GitHub Repository")
                ),
                new ToolIcon("powershell.svg", ToolIcon.MimeType.SVG_IMAGE, "PowerShell")
        );
    }

    @Override
    public List<ToolProperty> properties() {
        return List.of(
                ToolProperty.string("meshingress.powershell.executable")
                        .description("PowerShell executable used by native deployments.")
                        .defaultValue("pwsh")
                        .required(false),

                ToolProperty.longValue("meshingress.powershell.timeout-ms")
                        .description("Default PowerShell execution timeout in milliseconds.")
                        .defaultValue(20_000L)
                        .required(false)
        );
    }

    @Override
    public List<ToolRequirement> requirements() {
        return List.of(
                ToolRequirement.executable("pwsh")
                        .description("PowerShell 7+ executable must be available on PATH or configured through meshingress.powershell.executable.")
                        .required(true),
                ToolRequirement.scope(McpToolScope.SHELL_EXECUTE),
                ToolRequirement.scope(McpToolScope.FILES_WRITE)
        );
    }

    @Override
    public ToolReadme readme() {
        return ToolReadme.inline("""
                # PowerShell CLI
                
                Executes PowerShell scripts through the `powershell.cli.execute` MCP function.
                
                Native deployments require a PowerShell executable. By default, Meshingress uses `pwsh`.
                Override this through `meshingress.powershell.executable` when needed.
                Configure `resources/application.properties` beside this artifact when a native deployment needs a host-specific PowerShell executable or timeout default.
                """);
    }

}
