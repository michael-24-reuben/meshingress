package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchHandlerMethod;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
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
    private final ThreadPoolExecutor executor;

    public McpDispatchExecutor(MeshingressProperties properties, McpHandlerMethodInvoker methodInvoker) {
        this.properties = properties;
        this.methodInvoker = methodInvoker;
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
        Future<JsonNode> future;
        try {
            future = executor.submit(() -> methodInvoker.invoke(handler, params, context));
        } catch (RejectedExecutionException exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch is saturated.");
        }

        try {
            Duration timeout = properties.dispatch().defaultTimeout();
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch timed out.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch was interrupted.");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "MCP dispatch failed.");
        }
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
