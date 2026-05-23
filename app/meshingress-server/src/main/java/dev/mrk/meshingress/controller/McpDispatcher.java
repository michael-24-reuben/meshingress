package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import dev.mrk.meshingress.mcp.JsonRpcResponses;
import dev.mrk.meshingress.route.api.McpDispatchException;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchRegistry;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.Optional;

@Service
public class McpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpDispatcher.class);

    private final ObjectMapper objectMapper;
    private final JsonRpcResponses responses;
    private final McpDispatchRegistry dispatchRegistry;
    private final McpHandlerMethodInvoker methodInvoker;

    public McpDispatcher(
            ObjectMapper objectMapper,
            JsonRpcResponses responses,
            McpDispatchRegistry dispatchRegistry,
            McpHandlerMethodInvoker methodInvoker
    ) {
        this.objectMapper = objectMapper;
        this.responses = responses;
        this.dispatchRegistry = dispatchRegistry;
        this.methodInvoker = methodInvoker;
    }

    public Optional<JsonNode> dispatch(JsonNode request, McpCallContext context) {
        if (request == null || request.isNull()) {
            return Optional.of(responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Invalid request"));
        }
        if (request.isArray()) {
            return dispatchBatch((ArrayNode) request, context);
        }
        JsonNode response = dispatchSingle(request, context);
        return Optional.ofNullable(response);
    }

    private Optional<JsonNode> dispatchBatch(ArrayNode requests, McpCallContext context) {
        if (requests.isEmpty()) {
            return Optional.of(responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Batch request must not be empty"));
        }

        ArrayNode batchResponse = objectMapper.createArrayNode();
        for (JsonNode singleRequest : requests) {
            JsonNode response = dispatchSingle(singleRequest, context);
            if (response != null) {
                batchResponse.add(response);
            }
        }
        return batchResponse.isEmpty() ? Optional.empty() : Optional.of(batchResponse);
    }

    private JsonNode dispatchSingle(JsonNode request, McpCallContext context) {
        if (!request.isObject()) {
            return responses.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "JSON-RPC request must be an object");
        }

        JsonNode id = request.get("id");
        boolean notification = !request.has("id");

        try {
            if (!request.path("jsonrpc").asString("").equals("2.0")) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "jsonrpc must be \"2.0\"");
            }

            String method = request.path("method").asString("");
            if (method.isBlank()) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "method is required");
            }

            JsonNode result = dispatchMethod(method, request.path("params"), context);

            return notification ? null : responses.success(id, result);

        } catch (McpDispatchException exception) {
            return notification ? null : responses.error(id, exception.code(), exception.getMessage(), exception.data());
        } catch (JsonRpcException exception) {
            return notification ? null : responses.error(id, exception.code(), exception.getMessage(), exception.data());
        } catch (Exception exception) {
            LOGGER.warn("Unhandled MCP method exception", exception);
            return notification ? null : responses.error(id, JsonRpcErrorCodes.INTERNAL_ERROR, "Internal error");
        }
    }

    private JsonNode dispatchMethod(String method, JsonNode params, McpCallContext context) {
        return dispatchRegistry.find(method)
                .map(handler -> methodInvoker.invoke(handler, params, context))
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found"));
    }

}
