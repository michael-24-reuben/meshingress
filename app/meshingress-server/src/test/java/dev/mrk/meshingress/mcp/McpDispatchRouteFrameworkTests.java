package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import dev.mrk.meshingress.route.annotations.McpSchema;
import dev.mrk.meshingress.route.api.McpDispatchErrorCodes;
import dev.mrk.meshingress.route.api.McpDispatchException;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchMethodScanner;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchRegistry;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import dev.mrk.meshingress.route.framework.dispatch.McpReturnValueAdapter;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpCallContextArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpDispatchParamArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpProgressReporterArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpDispatchRouteFrameworkTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesAnnotatedSourcesAndBindsImplementationSeparatelyFromSchema() throws Exception {
        McpDispatchRegistry registry = scan(new EchoHandler());
        McpHandlerMethodInvoker invoker = invoker();
        McpCallContext context = new McpCallContext("Bearer token", "admin", "session-1", "request-1");
        JsonNode result = invoker.invoke(
                registry.find("test/echo").orElseThrow(),
                objectMapper.readTree("""
                                {
                                  "name": "sample",
                                  "arguments": {
                                    "message": "hello"
                                  }
                                }
                                """),
                context);

        assertTrue(registry.find("test/echo").isPresent());
        assertFalse(registry.find("test/missing").isPresent());
        assertEquals("sample", result.get("name").asString());
        assertEquals("hello", result.get("message").asString());
        assertEquals("test/echo", result.get("method").asString());
        assertEquals("request-1", result.get("requestId").asString());
        assertEquals("parameter[1]", registry.schemaRegistry().descriptorsFor("test/echo").get(1).location());
        assertEquals(ArgsSchema.class, registry.schemaRegistry().descriptorsFor("test/echo").get(1).schemaClass());
    }

    @Test
    void missingRequiredNameThrowsInvalidParams() throws Exception {
        McpDispatchRegistry registry = scan(new EchoHandler());
        McpHandlerMethodInvoker invoker = invoker();

        McpDispatchException exception = assertThrows(McpDispatchException.class, () -> invoker.invoke(
                registry.find("test/echo").orElseThrow(),
                objectMapper.readTree("""
                                {
                                  "arguments": {
                                    "message": "hello"
                                  }
                                }
                                """),
                new McpCallContext(null, null, null, null)));

        assertEquals(McpDispatchErrorCodes.INVALID_PARAMS, exception.code());
    }

    @Test
    void nonObjectArgsThrowInvalidParamsForObjectNodeTargets() throws Exception {
        McpDispatchRegistry registry = scan(new EchoHandler());
        McpHandlerMethodInvoker invoker = invoker();

        McpDispatchException exception = assertThrows(McpDispatchException.class, () -> invoker.invoke(
                registry.find("test/object-args").orElseThrow(),
                objectMapper.readTree("""
                                {
                                  "arguments": "not-an-object"
                                }
                                """),
                new McpCallContext(null, null, null, null)));

        assertEquals(McpDispatchErrorCodes.INVALID_PARAMS, exception.code());
    }

    @Test
    void duplicateAnnotationMappingsFailDuringScan() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scan(new EchoHandler(), new DuplicateEchoHandler())
        );

        assertTrue(exception.getMessage().contains("Duplicate MCP annotation mapping 'test/echo'"));
    }

    @Test
    void injectsAFreshProgressReporterWithoutMakingItAClientParameter() throws Exception {
        ProgressHandler handler = new ProgressHandler();
        McpDispatchRegistry registry = scan(handler);
        McpHandlerMethodInvoker invoker = invoker();
        JsonNode params = objectMapper.readTree("{}");

        JsonNode firstResult = invoker.invoke(
                registry.find("test/progress").orElseThrow(),
                params,
                new McpCallContext(null, null, null, null));
        JsonNode secondResult = invoker.invoke(
                registry.find("test/progress").orElseThrow(),
                params,
                new McpCallContext(null, null, null, null));

        assertTrue(firstResult.get("reporterInjected").asBoolean());
        assertTrue(secondResult.get("reporterInjected").asBoolean());
        assertEquals(2, handler.reporters.size());
        assertNotSame(handler.reporters.getFirst(), handler.reporters.getLast());
    }

    private McpDispatchRegistry scan(Object... handlers) {
        return new McpDispatchMethodScanner().scan(List.of(handlers));
    }

    private McpHandlerMethodInvoker invoker() {
        return new McpHandlerMethodInvoker(
                objectMapper,
                List.of(
                        new McpCallContextArgumentResolver(),
                        new McpProgressReporterArgumentResolver(),
                        new McpDispatchParamArgumentResolver(new TypedJsonArgumentBinder())
                ),
                new McpReturnValueAdapter()
        );
    }

    interface EchoArgs {
        String message();
    }

    record EchoArgsImpl(String message) implements EchoArgs {
    }

    static final class ArgsSchema {
    }

    static final class MethodSchema {
    }

    @McpDispatchMapping("/test/")
    static class EchoHandler {

        @McpDispatchMethod("/echo")
        @McpSchema(MethodSchema.class)
        Map<String, Object> echo(
                @McpDispatchParam("name") String name,
                @McpDispatchParam(value = "args", implementation = EchoArgsImpl.class)
                @McpSchema(ArgsSchema.class)
                EchoArgs args,
                @McpDispatchParam("params") ObjectNode params,
                @McpDispatchParam("method") String method,
                McpCallContext context
        ) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", name);
            result.put("message", args.message());
            result.put("hasParams", params.isObject());
            result.put("method", method);
            result.put("requestId", context.requestId());
            return result;
        }

        @McpDispatchMethod("object-args")
        ObjectNode objectArgs(@McpDispatchParam("args") ObjectNode args) {
            return args;
        }
    }

    @McpDispatchMapping("test")
    static class DuplicateEchoHandler {

        @McpDispatchMethod("echo")
        Map<String, Object> echo(@McpDispatchParam("method") String method) {
            return Map.of("method", method);
        }
    }

    @McpDispatchMapping("test")
    static class ProgressHandler {

        private final List<McpProgressReporter> reporters = new java.util.ArrayList<>();

        @McpDispatchMethod("progress")
        Map<String, Object> progress(McpProgressReporter progress) {
            reporters.add(progress);
            progress.complete("No-op progress reporter was injected");
            return Map.of("reporterInjected", true);
        }
    }
}
