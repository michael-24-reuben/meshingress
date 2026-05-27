package dev.mrk.meshingress.runtime.loader;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

public interface ToolModuleHandlerFactory {
    List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext);
}
