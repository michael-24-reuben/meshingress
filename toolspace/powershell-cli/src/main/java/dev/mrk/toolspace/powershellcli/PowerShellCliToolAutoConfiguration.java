package dev.mrk.toolspace.powershellcli;

import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class PowerShellCliToolAutoConfiguration {

    @Bean
    PowerShellCliManifest powerShellCliManifest() {
        return new PowerShellCliManifest();
    }

    @Bean
    PowerShellCliTool powerShellCliToolTool(
            ObjectMapper objectMapper,
            McpToolMetadata mcpToolMetadata,
            PowerShellCliManifest manifest
    ) {
        mcpToolMetadata.registerManifest(PowerShellCliTool.class, manifest);
        return new PowerShellCliTool(objectMapper, mcpToolMetadata);
    }
}
