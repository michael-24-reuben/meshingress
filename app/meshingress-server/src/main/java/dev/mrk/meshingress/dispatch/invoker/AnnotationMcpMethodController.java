package dev.mrk.meshingress.dispatch.invoker;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.controller.McpMethodController;
import dev.mrk.meshingress.dispatch.annotation.McpDispatchMapping;
import dev.mrk.meshingress.dispatch.resolver.McpCallContextArgumentResolver;
import dev.mrk.meshingress.dispatch.resolver.McpDispatchArgumentResolver;
import dev.mrk.meshingress.dispatch.resolver.McpDispatchParamArgumentResolver;
import dev.mrk.meshingress.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

@Component
public class AnnotationMcpMethodController implements McpMethodController {

    private final McpDispatchRegistry registry;
    private final McpHandlerMethodInvoker invoker;

    @Autowired
    public AnnotationMcpMethodController(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.registry = new McpDispatchMethodScanner().scan(
                applicationContext.getBeansWithAnnotation(McpDispatchMapping.class).values()
        );
        TypedJsonArgumentBinder binder = new TypedJsonArgumentBinder();
        List<McpDispatchArgumentResolver> resolvers = List.of(
                new McpCallContextArgumentResolver(),
                new McpDispatchParamArgumentResolver(binder)
        );
        this.invoker = new McpHandlerMethodInvoker(objectMapper, resolvers, new McpReturnValueAdapter());
    }

    public AnnotationMcpMethodController(McpDispatchRegistry registry, McpHandlerMethodInvoker invoker) {
        this.registry = registry;
        this.invoker = invoker;
    }

    @Override
    public Set<String> supportedMethods() {
        return registry.supportedMethods();
    }

    @Override
    public boolean supports(String method) {
        return registry.find(method).isPresent();
    }

    @Override
    public JsonNode dispatch(String method, JsonNode params, McpCallContext context) {
        McpDispatchHandlerMethod handler = registry.find(method)
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found"));
        return invoker.invoke(handler, params, context);
    }

    public McpDispatchRegistry registry() {
        return registry;
    }
}
