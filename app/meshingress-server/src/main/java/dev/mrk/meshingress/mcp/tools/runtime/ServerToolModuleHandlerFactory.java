package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.mcp.tools.annotation.AnnotatedMcpToolHandler;
import dev.mrk.meshingress.mcp.tools.cache.McpCacheManager;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.runtime.loader.ToolModuleHandlerFactory;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ServerToolModuleHandlerFactory implements ToolModuleHandlerFactory {

    private final ObjectMapper objectMapper;
    private final McpCacheManager cacheManager;

    public ServerToolModuleHandlerFactory(ObjectMapper objectMapper, McpCacheManager cacheManager) {
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    // Used during the staging and experimental strategies phase
    @Override
    public List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext) {
        List<McpToolHandler> handlers = new ArrayList<>(moduleContext.getBeanFactory()
                .getBeansOfType(McpToolHandler.class)
                .values());

        McpToolAnnotationScanner scanner = new McpToolAnnotationScanner(objectMapper);
        TypedJsonArgumentBinder argumentBinder = new TypedJsonArgumentBinder();
        for (Map.Entry<String, Object> entry : moduleContext.getBeanFactory().getBeansWithAnnotation(McpTool.class).entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof McpToolHandler) {
                continue;
            }
            AnnotatedMcpTool tool = scanner.scan(ClassUtils.getUserClass(bean));
            for (var function : tool.functions()) {
                handlers.add(new AnnotatedMcpToolHandler(bean, tool, function, objectMapper, argumentBinder, cacheManager));
            }
        }
        return List.copyOf(handlers);
    }
}
