package dev.mrk.meshingress.mcp.tools.annotation;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.mcp.tools.cache.McpCacheManager;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AnnotatedMcpToolHandlerProvider {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final McpCacheManager cacheManager;
    private final McpToolMetadata toolMetadata;

    public AnnotatedMcpToolHandlerProvider(
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            McpCacheManager cacheManager,
            McpToolMetadata toolMetadata
    ) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.toolMetadata = toolMetadata;
    }

    public List<McpToolHandler> handlers() {
        TypedJsonArgumentBinder argumentBinder = new TypedJsonArgumentBinder();
        List<McpToolHandler> handlers = new ArrayList<>();
        for (Map.Entry<String, Object> entry : applicationContext.getBeansWithAnnotation(McpTool.class).entrySet()) {
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
