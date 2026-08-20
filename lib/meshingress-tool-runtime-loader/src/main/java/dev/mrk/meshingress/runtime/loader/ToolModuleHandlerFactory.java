package dev.mrk.meshingress.runtime.loader;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

public interface ToolModuleHandlerFactory {
    List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext);

    default List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext, ToolModuleId moduleId) {
        return handlers(moduleContext);
    }
}
