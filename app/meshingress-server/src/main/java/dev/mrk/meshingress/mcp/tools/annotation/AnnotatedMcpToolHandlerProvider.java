package dev.mrk.meshingress.mcp.tools.annotation;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.tools.framework.scanner.McpToolAnnotationScanner;
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

    public AnnotatedMcpToolHandlerProvider(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    public List<McpToolHandler> handlers() {
        McpToolAnnotationScanner scanner = new McpToolAnnotationScanner(objectMapper);
        TypedJsonArgumentBinder argumentBinder = new TypedJsonArgumentBinder();
        List<McpToolHandler> handlers = new ArrayList<>();
        for (Map.Entry<String, Object> entry : applicationContext.getBeansWithAnnotation(McpTool.class).entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof McpToolHandler) {
                continue;
            }
            AnnotatedMcpTool tool = scanner.scan(ClassUtils.getUserClass(bean));
            for (var function : tool.functions()) {
                handlers.add(new AnnotatedMcpToolHandler(bean, tool, function, objectMapper, argumentBinder));
            }
        }
        return List.copyOf(handlers);
    }
}
