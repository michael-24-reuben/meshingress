package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.McpInvocation;
import dev.mrk.meshingress.mcp.McpProgressLifecycle;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchHandlerMethod;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpDispatchExecutorTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toolsCallUsesFunctionTimeoutInsteadOfTheGlobalDispatchDefault() throws Exception {
        ToolRegistry registry = mock(ToolRegistry.class);
        ObjectNode annotations = objectMapper.createObjectNode().put("timeoutMs", 250);
        McpFunctionDescriptor function = new McpFunctionDescriptor(
                "open-ink-library.toonverse.download-book", "Download", "", 1, true, ToolVisibility.PUBLIC,
                "open-ink-library.toonverse.download-book", objectMapper.createObjectNode(), null, annotations, false
        );
        when(registry.findEnabledFunction("open-ink-library.toonverse.download-book")).thenReturn(Optional.of(function));

        McpHandlerMethodInvoker invoker = mock(McpHandlerMethodInvoker.class);
        ObjectNode expected = objectMapper.createObjectNode().put("completed", true);
        when(invoker.invoke(any(), any(), any())).thenAnswer(ignored -> {
            Thread.sleep(75);
            return expected;
        });

        MeshingressProperties properties = new MeshingressProperties(
                null, null, null,
                new MeshingressProperties.Dispatch(Duration.ofMillis(25), 1, 0, true, false, true, true),
                null, null, null, null, null, null, null
        );
        McpDispatchExecutor executor = new McpDispatchExecutor(properties, invoker, registry);
        try {
            Method method = TestToolsController.class.getDeclaredMethod("call");
            McpDispatchHandlerMethod handler = new McpDispatchHandlerMethod("tools/call", new TestToolsController(), method, List.of());
            ObjectNode params = objectMapper.createObjectNode().put("name", "open-ink-library.toonverse.download-book");

            assertEquals(expected, executor.execute(handler, params, new McpCallContext(null, null, "session-1", "request-1")));
        } finally {
            executor.destroy();
        }
    }

    @Test
    void websocketProgressPlanDefersTheGraceTimeoutUntilAfterTheEstimate() throws Exception {
        ToolRegistry registry = mock(ToolRegistry.class);
        ObjectNode annotations = objectMapper.createObjectNode()
                .put("timeoutMs", 25)
                .put("progressReporter", true);
        McpFunctionDescriptor function = function(annotations);
        when(registry.findEnabledFunction("open-ink-library.toonverse.download-book")).thenReturn(Optional.of(function));

        McpHandlerMethodInvoker invoker = mock(McpHandlerMethodInvoker.class);
        ObjectNode expected = objectMapper.createObjectNode().put("completed", true);
        when(invoker.invoke(any(), any(), any())).thenAnswer(invocation -> {
            McpCallContext context = invocation.getArgument(2);
            context.progressReporter().plan(Duration.ofMillis(75), 1, List.of("downloading"), "Starting");
            Thread.sleep(50);
            return expected;
        });

        McpDispatchExecutor executor = new McpDispatchExecutor(properties(Duration.ofMillis(200)), invoker, registry);
        McpProgressLifecycle lifecycle = new McpProgressLifecycle(ignored -> { });
        McpCallContext context = new McpCallContext(null, null, "session-1", "request-1", lifecycle.reporter());
        try {
            assertEquals(expected, executor.execute(toolsCallHandler(), toolParams(), McpInvocation.webSocket(context, lifecycle)));
        } finally {
            executor.destroy();
        }
    }

    @Test
    void websocketProgressCapableFunctionWithoutAPlanUsesThePrePlanGuard() throws Exception {
        ToolRegistry registry = mock(ToolRegistry.class);
        ObjectNode annotations = objectMapper.createObjectNode()
                .put("timeoutMs", 250)
                .put("progressReporter", true);
        when(registry.findEnabledFunction("open-ink-library.toonverse.download-book")).thenReturn(Optional.of(function(annotations)));

        McpHandlerMethodInvoker invoker = mock(McpHandlerMethodInvoker.class);
        when(invoker.invoke(any(), any(), any())).thenAnswer(ignored -> {
            Thread.sleep(100);
            return objectMapper.createObjectNode();
        });

        McpDispatchExecutor executor = new McpDispatchExecutor(properties(Duration.ofMillis(20)), invoker, registry);
        McpProgressLifecycle lifecycle = new McpProgressLifecycle(ignored -> { });
        McpCallContext context = new McpCallContext(null, null, "session-1", "request-1", lifecycle.reporter());
        try {
            JsonRpcException exception = assertThrows(
                    JsonRpcException.class,
                    () -> executor.execute(toolsCallHandler(), toolParams(), McpInvocation.webSocket(context, lifecycle))
            );
            assertEquals("MCP WebSocket dispatch did not report a progress plan.", exception.getMessage());
        } finally {
            executor.destroy();
        }
    }

    private McpFunctionDescriptor function(ObjectNode annotations) {
        return new McpFunctionDescriptor(
                "open-ink-library.toonverse.download-book", "Download", "", 1, true, ToolVisibility.PUBLIC,
                "open-ink-library.toonverse.download-book", objectMapper.createObjectNode(), null, annotations, false
        );
    }

    private MeshingressProperties properties(Duration defaultTimeout) {
        return new MeshingressProperties(
                null, null, null,
                new MeshingressProperties.Dispatch(defaultTimeout, 1, 0, true, false, true, true),
                null, null, null, null, null, null, null
        );
    }

    private McpDispatchHandlerMethod toolsCallHandler() throws Exception {
        Method method = TestToolsController.class.getDeclaredMethod("call");
        return new McpDispatchHandlerMethod("tools/call", new TestToolsController(), method, List.of());
    }

    private ObjectNode toolParams() {
        return objectMapper.createObjectNode().put("name", "open-ink-library.toonverse.download-book");
    }

    static final class TestToolsController {
        ObjectNode call() { return null; }
    }
}
