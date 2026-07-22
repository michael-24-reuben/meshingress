package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcResponses;
import dev.mrk.meshingress.mcp.McpInvocation;
import dev.mrk.meshingress.mcp.McpInvocationFactory;
import dev.mrk.meshingress.mcp.audit.McpClientTraceService;
import dev.mrk.meshingress.route.api.McpDispatchException;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchRegistry;
import dev.mrk.meshingress.security.McpAccessPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;

@Service
public class McpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpDispatcher.class);

    private final ObjectMapper objectMapper;
    private final JsonRpcResponses responses;
    private final McpDispatchRegistry dispatchRegistry;
    private final McpDispatchExecutor dispatchExecutor;
    private final MeshingressProperties properties;
    private final McpAccessPolicyService accessPolicyService;
    private final McpClientTraceService clientTraceService;

    public McpDispatcher(
            ObjectMapper objectMapper,
            JsonRpcResponses responses,
            McpDispatchRegistry dispatchRegistry,
            McpDispatchExecutor dispatchExecutor,
            MeshingressProperties properties,
            McpAccessPolicyService accessPolicyService,
            McpClientTraceService clientTraceService
    ) {
        this.objectMapper = objectMapper;
        this.responses = responses;
        this.dispatchRegistry = dispatchRegistry;
        this.dispatchExecutor = dispatchExecutor;
        this.properties = properties;
        this.accessPolicyService = accessPolicyService;
        this.clientTraceService = clientTraceService;
    }

    public Optional<JsonNode> dispatch(JsonNode request, McpCallContext context) {
        return dispatch(request, McpInvocationFactory.http(context));
    }

    public Optional<JsonNode> dispatch(JsonNode request, McpInvocationFactory invocationFactory) {
        if (request == null || request.isNull()) {
            return Optional.of(responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Invalid request"));
        }
        if (request.isArray()) {
            return dispatchBatch((ArrayNode) request, invocationFactory);
        }
        JsonNode response = dispatchSingle(request, invocationFactory);
        return Optional.ofNullable(response);
    }

    private Optional<JsonNode> dispatchBatch(ArrayNode requests, McpInvocationFactory invocationFactory) {
        if (requests.isEmpty()) {
            return Optional.of(responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Batch request must not be empty"));
        }

        ArrayNode batchResponse = objectMapper.createArrayNode();
        for (JsonNode singleRequest : requests) {
            JsonNode response = dispatchSingle(singleRequest, invocationFactory);
            if (response != null) {
                batchResponse.add(response);
            }
        }
        return batchResponse.isEmpty() ? Optional.empty() : Optional.of(batchResponse);
    }

    private JsonNode dispatchSingle(JsonNode request, McpInvocationFactory invocationFactory) {
        if (!request.isObject()) {
            return responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "JSON-RPC request must be an object");
        }

        McpInvocation invocation = invocationFactory.create(request);
        McpCallContext context = invocation.context();

        JsonNode id = request.get("id");
        boolean notification = !request.has("id");

        try {
            clientTraceService.record(request, context);
            if (!request.path("jsonrpc").asString("").equals("2.0")) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "jsonrpc must be \"2.0\"");
            }

            String method = request.path("method").asString("");
            if (method.isBlank()) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "method is required");
            }

            accessPolicyService.requireAuthenticated(context);
            JsonNode result = dispatchMethod(method, request.path("params"), invocation);

            return notification ? null : responses.success(id, result);

        } catch (McpDispatchException exception) {
            return notification ? null : dispatchError(id, exception.code(), exception.getMessage(), exception.data(), exception);
        } catch (JsonRpcException exception) {
            return notification ? null : dispatchError(id, exception.code(), exception.getMessage(), exception.data(), exception);
        } catch (Exception exception) {
            LOGGER.warn("Unhandled MCP method exception", exception);
            return notification ? null : dispatchError(id, JsonRpcErrorCodes.INTERNAL_ERROR, "Internal error", null, exception);
        }
    }

    private JsonNode dispatchMethod(String method, JsonNode params, McpInvocation invocation) {
        return dispatchRegistry.find(method)
                .map(handler -> dispatchExecutor.execute(handler, params, invocation))
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found"));
    }

    private JsonNode dispatchError(JsonNode id, int code, String message, JsonNode data, Throwable exception) {
        String responseMessage = properties.dispatch().redactErrors() && code == JsonRpcErrorCodes.INTERNAL_ERROR
                ? "Internal error"
                : message;
        JsonNode responseData = data;
        if (properties.dispatch().includeStacktrace()) {
            ObjectNode stacktrace = objectMapper.createObjectNode();
            stacktrace.put("exception", exception.getClass().getName());
            stacktrace.put("message", exception.getMessage() == null ? "" : exception.getMessage());
            if (responseData != null && responseData.isObject()) {
                ObjectNode merged = ((ObjectNode) responseData).deepCopy();
                merged.set("stacktrace", stacktrace);
                responseData = merged;
            } else {
                ObjectNode wrapper = objectMapper.createObjectNode();
                if (responseData != null) {
                    wrapper.set("data", responseData);
                }
                wrapper.set("stacktrace", stacktrace);
                responseData = wrapper;
            }
        }
        return responses.error(id, code, responseMessage, responseData);
    }

}
