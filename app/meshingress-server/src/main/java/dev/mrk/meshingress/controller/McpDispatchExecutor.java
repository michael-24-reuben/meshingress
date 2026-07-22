package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.McpInvocation;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchHandlerMethod;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class McpDispatchExecutor implements DisposableBean {

    private final MeshingressProperties properties;
    private final McpHandlerMethodInvoker methodInvoker;
    private final ToolRegistry toolRegistry;
    private final ThreadPoolExecutor executor;

    public McpDispatchExecutor(MeshingressProperties properties, McpHandlerMethodInvoker methodInvoker, ToolRegistry toolRegistry) {
        this.properties = properties;
        this.methodInvoker = methodInvoker;
        this.toolRegistry = toolRegistry;
        int maxConcurrentCalls = properties.dispatch().maxConcurrentCalls();
        this.executor = new ThreadPoolExecutor(
                maxConcurrentCalls,
                maxConcurrentCalls,
                0L,
                TimeUnit.MILLISECONDS,
                properties.dispatch().queueCapacity() == 0
                        ? new SynchronousQueue<>()
                        : new ArrayBlockingQueue<>(properties.dispatch().queueCapacity()),
                runnable -> {
                    Thread thread = new Thread(runnable, "meshingress-mcp-dispatch");
                    thread.setDaemon(true);
                    return thread;
                },
                properties.dispatch().rejectWhenSaturated()
                        ? new ThreadPoolExecutor.AbortPolicy()
                        : new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public JsonNode execute(McpDispatchHandlerMethod handler, JsonNode params, McpCallContext context) {
        return execute(handler, params, McpInvocation.http(context));
    }

    public JsonNode execute(McpDispatchHandlerMethod handler, JsonNode params, McpInvocation invocation) {
        McpCallContext context = invocation.context();
        CompletableFuture<JsonNode> completion = new CompletableFuture<>();
        Future<JsonNode> future;
        try {
            future = executor.submit(() -> {
                try {
                    JsonNode result = methodInvoker.invoke(handler, params, context);
                    completion.complete(result);
                    return result;
                } catch (Throwable exception) {
                    completion.completeExceptionally(exception);
                    return null;
                }
            });
        } catch (RejectedExecutionException exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch is saturated.");
        }

        try {
            TimeoutPolicy policy = timeoutFor(handler, params);
            if (invocation.transport() == McpInvocation.Transport.WEBSOCKET
                    && policy.progressReporter()
                    && invocation.progressLifecycle() != null) {
                return awaitWebSocketCompletion(future, completion, invocation, policy.timeout());
            }
            return awaitCompletion(future, completion, policy.timeout());
        } finally {
            if (invocation.progressLifecycle() != null && !invocation.progressLifecycle().terminalReported()) {
                invocation.progressLifecycle().close();
            }
        }
    }

    private JsonNode awaitWebSocketCompletion(
            Future<JsonNode> future,
            CompletableFuture<JsonNode> completion,
            McpInvocation invocation,
            Duration graceTimeout
    ) {
        try {
            CompletableFuture.anyOf(completion, invocation.progressLifecycle().estimatedDuration())
                    .get(properties.dispatch().defaultTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (completion.isDone()) {
                return completedResult(completion);
            }
            Duration estimate = invocation.progressLifecycle().estimatedDuration().join();
            return awaitCompletion(future, completion, estimate.plus(graceTimeout));
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP WebSocket dispatch did not report a progress plan.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch was interrupted.");
        } catch (ExecutionException exception) {
            throw failedDispatch(exception.getCause());
        }
    }

    private JsonNode awaitCompletion(Future<JsonNode> future, CompletableFuture<JsonNode> completion, Duration timeout) {
        try {
            return completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch timed out.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch was interrupted.");
        } catch (ExecutionException exception) {
            throw failedDispatch(exception.getCause());
        }
    }

    private JsonNode completedResult(CompletableFuture<JsonNode> completion) {
        try {
            return completion.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch was interrupted.");
        } catch (ExecutionException exception) {
            throw failedDispatch(exception.getCause());
        }
    }

    private RuntimeException failedDispatch(Throwable cause) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch failed.");
    }

    private TimeoutPolicy timeoutFor(McpDispatchHandlerMethod handler, JsonNode params) {
        Duration fallback = properties.dispatch().defaultTimeout();
        if (!"tools/call".equals(handler.methodName()) || !params.isObject()) {
            return new TimeoutPolicy(fallback, false);
        }
        String functionName = params.path("name").asString("");
        if (functionName.isBlank()) {
            return new TimeoutPolicy(fallback, false);
        }
        return toolRegistry.findEnabledFunction(functionName)
                .map(this::configuredTimeoutPolicy)
                .orElse(new TimeoutPolicy(fallback, false));
    }

    private TimeoutPolicy configuredTimeoutPolicy(McpFunctionDescriptor function) {
        JsonNode annotations = function.annotations();
        if (annotations == null || !annotations.path("timeoutMs").canConvertToLong()) {
            return new TimeoutPolicy(properties.dispatch().defaultTimeout(), false);
        }
        long timeoutMs = annotations.path("timeoutMs").asLong();
        Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : properties.dispatch().defaultTimeout();
        return new TimeoutPolicy(timeout, annotations.path("progressReporter").asBoolean(false));
    }

    private record TimeoutPolicy(Duration timeout, boolean progressReporter) {
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
