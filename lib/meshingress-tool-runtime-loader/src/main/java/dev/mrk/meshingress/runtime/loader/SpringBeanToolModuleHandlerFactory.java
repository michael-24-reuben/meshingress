package dev.mrk.meshingress.runtime.loader;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

public class SpringBeanToolModuleHandlerFactory implements ToolModuleHandlerFactory {

    @Override
    public List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext) {
        return List.copyOf(moduleContext.getBeanFactory().getBeansOfType(McpToolHandler.class).values());
    }
}
