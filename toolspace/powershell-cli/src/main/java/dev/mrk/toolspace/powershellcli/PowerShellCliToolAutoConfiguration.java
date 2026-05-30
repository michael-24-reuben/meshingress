package dev.mrk.toolspace.powershellcli;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class PowerShellCliToolAutoConfiguration {

    @Bean
    PowerShellCliTool powerShellCliToolTool(ObjectMapper objectMapper) {
        return new PowerShellCliTool(objectMapper);
    }
}
