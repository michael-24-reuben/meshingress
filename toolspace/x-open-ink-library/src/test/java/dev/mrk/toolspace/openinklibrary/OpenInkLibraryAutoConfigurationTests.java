package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseTool;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenInkLibraryAutoConfigurationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenInkLibraryAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(McpToolMetadata.class, McpToolMetadata::new);

    @Test
    void registersOnlyTheToonverseSourceTool() {
        contextRunner.run(context -> {
            var tools = context.getBeansWithAnnotation(McpTool.class);

            assertEquals(1, tools.size());
            assertEquals(ToonverseTool.class, tools.get("toonverseTool").getClass());
        });
    }
}
