package dev.mrk.meshingress.config;

import dev.mrk.meshingress.controller.McpController;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.framework.AnnotatedMcpRoute;
import dev.mrk.meshingress.route.framework.McpRouteAnnotationScanner;
import dev.mrk.meshingress.route.framework.McpRouteRegistry;
import dev.mrk.meshingress.route.framework.McpRouteValidator;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchMethodScanner;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchRegistry;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import dev.mrk.meshingress.route.framework.dispatch.McpReturnValueAdapter;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpCallContextArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpDispatchArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpDispatchParamArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class McpRouteConfiguration {

    @Bean
    McpRouteRegistry mcpRouteRegistry() {
        List<AnnotatedMcpRoute> routes = new McpRouteAnnotationScanner().scan(List.of(McpController.class));
        new McpRouteValidator().validateOrThrow(routes);
        return new McpRouteRegistry(routes);
    }

    @Bean
    McpDispatchRegistry mcpDispatchRegistry(ApplicationContext applicationContext) {
        return new McpDispatchMethodScanner().scan(
                applicationContext.getBeansWithAnnotation(McpDispatchMapping.class).values()
        );
    }

    @Bean
    McpHandlerMethodInvoker mcpHandlerMethodInvoker(ObjectMapper objectMapper) {
        TypedJsonArgumentBinder binder = new TypedJsonArgumentBinder();
        List<McpDispatchArgumentResolver> resolvers = List.of(
                new McpCallContextArgumentResolver(),
                new McpDispatchParamArgumentResolver(binder)
        );
        return new McpHandlerMethodInvoker(objectMapper, resolvers, new McpReturnValueAdapter());
    }
}
