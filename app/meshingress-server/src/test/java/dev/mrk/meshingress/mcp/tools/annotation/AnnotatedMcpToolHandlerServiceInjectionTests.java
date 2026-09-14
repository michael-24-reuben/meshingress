package dev.mrk.meshingress.mcp.tools.annotation;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.services.McpServiceLifecycle;
import dev.mrk.meshingress.api.services.McpServiceResolver;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionService;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.mcp.tools.cache.McpCacheManager;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnotatedMcpToolHandlerServiceInjectionTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void injectsTheSelectedServiceFromTheCallContext() {
        ServiceInjectionTool toolBean = new ServiceInjectionTool();
        AnnotatedMcpTool scannedTool = new McpToolAnnotationScanner(objectMapper, "test").scan(ServiceInjectionTool.class);
        AnnotatedMcpToolHandler handler = new AnnotatedMcpToolHandler(
                toolBean,
                scannedTool,
                scannedTool.functions().getFirst(),
                objectMapper,
                new TypedJsonArgumentBinder(),
                new McpCacheManager(null, null, null, null)
        );
        SelectedGreetingService selected = new SelectedGreetingService();
        McpServiceResolver services = new McpServiceResolver() {
            @Override
            public <T> Optional<T> find(Class<T> contract) {
                return Optional.empty();
            }

            @Override
            public <T> List<T> findAll(Class<T> contract) {
                return List.of(contract.cast(new AlternateGreetingService()), contract.cast(selected));
            }
        };
        ObjectNode arguments = objectMapper.createObjectNode().put("name", "Ada");

        DispatchExecutionResult result = handler.call(
                arguments,
                new McpCallContext("Bearer one", "admin", "session-a", "request-id").withServices(services)
                        .withServiceRequirements(Map.of("greeting-service", Map.of("prefix", "configured")))
        );

        assertEquals("configured:Ada", result.structuredContent().orElseThrow().path("data").path("greeting").asString());
        assertEquals(List.of("init:configured", "stop:configured"), selected.lifecycleEvents);
    }

    @McpTool("service-injection")
    static class ServiceInjectionTool {

        @McpFunction("greet")
        Map<String, String> greet(
                @McpFunctionParam("name") String name,
                @McpFunctionService(
                        value = "greeting-service",
                        oneOf = SelectedGreetingService.class
                ) GreetingService service
        ) {
            return Map.of("greeting", service.greet(name));
        }
    }

    interface GreetingService extends McpServiceLifecycle {
        String greet(String name);
    }

    static class SelectedGreetingService implements GreetingService {
        private final List<String> lifecycleEvents = new java.util.ArrayList<>();
        private String prefix = "selected";

        void init(@McpFunctionParam("prefix") String prefix, McpCallContext context) {
            this.prefix = prefix;
            lifecycleEvents.add("init:" + this.prefix);
        }

        @Override
        public void stop() {
            lifecycleEvents.add("stop:" + prefix);
        }

        @Override
        public String greet(String name) {
            return prefix + ":" + name;
        }
    }

    static class AlternateGreetingService implements GreetingService {
        @Override
        public String greet(String name) {
            return "alternate:" + name;
        }
    }
}
