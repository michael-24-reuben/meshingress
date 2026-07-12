package dev.mrk.toolspace.powershellcli;

import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.ToolLink;
import dev.mrk.meshingress.toolmetadata.ToolProperty;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;

import java.util.List;

public final class PowerShellCliManifest implements McpToolManifestDefinition {

    @Override
    public String toolId() {
        return "cli.powershell";
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
    public List<ToolLink> links() {
        return List.of(
                ToolLink.documentation("https://learn.microsoft.com/powershell/")
                        .label("PowerShell Documentation"),

                ToolLink.source("https://github.com/PowerShell/PowerShell")
                        .label("PowerShell GitHub Repository")
        );
    }

    @Override
    public String readmeMarkdown() {
        return """
                # PowerShell CLI
                
                Executes PowerShell scripts through the `cli.powershell.execute` MCP function.
                
                Native deployments require a PowerShell executable. By default, Meshingress uses `pwsh`.
                Override this through `meshingress.powershell.executable` when needed.
                Configure `resources/application.yaml` beside this artifact when a native deployment needs a host-specific PowerShell executable or timeout default.
                """;
    }
}
