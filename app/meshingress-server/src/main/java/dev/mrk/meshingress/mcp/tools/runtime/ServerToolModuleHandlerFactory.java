package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.mcp.tools.annotation.AnnotatedMcpToolHandler;
import dev.mrk.meshingress.mcp.tools.cache.McpCacheManager;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.runtime.loader.ToolModuleHandlerFactory;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolcatalog.ToolModuleCatalog;
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
    private final McpToolMetadata toolMetadata;
    private final ToolModuleCatalog moduleCatalog;

    public ServerToolModuleHandlerFactory(ObjectMapper objectMapper, McpCacheManager cacheManager, McpToolMetadata toolMetadata, ToolModuleCatalog moduleCatalog) {
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.toolMetadata = toolMetadata;
        this.moduleCatalog = moduleCatalog;
    }

    // Used during the staging and experimental strategies phase
    @Override
    public List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext) {
        return handlers(moduleContext, null);
    }

    @Override
    public List<McpToolHandler> handlers(ConfigurableApplicationContext moduleContext, ToolModuleId moduleId) {
        if (moduleId != null) {
            moduleCatalog.registerRuntimeModule(moduleId, moduleContext.getBeanFactory().getBeansOfType(McpToolManifestDefinition.class).values());
        }
        List<McpToolHandler> handlers = new ArrayList<>(moduleContext.getBeanFactory()
                .getBeansOfType(McpToolHandler.class)
                .values());

        TypedJsonArgumentBinder argumentBinder = new TypedJsonArgumentBinder();
        for (Map.Entry<String, Object> entry : moduleContext.getBeanFactory().getBeansWithAnnotation(McpTool.class).entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof McpToolHandler) {
                continue;
            }
            Class<?> toolClass = ClassUtils.getUserClass(bean);
            AnnotatedMcpTool tool = new McpToolAnnotationScanner(objectMapper, toolMetadata.moduleMetadata(toolClass).namespace()).scan(toolClass);
            for (var function : tool.functions()) {
                handlers.add(new AnnotatedMcpToolHandler(bean, tool, function, objectMapper, argumentBinder, cacheManager));
            }
        }
        return List.copyOf(handlers);
    }
}
