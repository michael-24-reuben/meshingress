package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import dev.mrk.meshingress.mcp.JsonRpcResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class McpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpDispatcher.class);

    private final ObjectMapper objectMapper;
    private final JsonRpcResponses responses;
    private final List<McpMethodController> methodControllers;

    public McpDispatcher(
            ObjectMapper objectMapper,
            JsonRpcResponses responses,
            List<McpMethodController> methodControllers
    ) {
        this.objectMapper = objectMapper;
        this.responses = responses;
        this.methodControllers = List.copyOf(methodControllers);
        validateUniqueMethodOwners(this.methodControllers);
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
        if (!request.path("jsonrpc").asString("").equals("2.0")) {
            return responses.error(id, JsonRpcErrorCodes.INVALID_REQUEST, "jsonrpc must be \"2.0\"");
        }
        String method = request.path("method").asString("");
        if (method.isBlank()) {
            return responses.error(id, JsonRpcErrorCodes.INVALID_REQUEST, "method is required");
        }

        JsonNode result;
        try {
            result = dispatchMethod(method, request.path("params"), context);
        } catch (JsonRpcException exception) {
            return responses.error(id, exception.code(), exception.getMessage(), exception.data());
        } catch (Exception exception) {
            LOGGER.warn("Unhandled MCP method exception for {}", method, exception);
            return responses.error(id, JsonRpcErrorCodes.INTERNAL_ERROR, "Internal error");
        }

        if (notification) {
            return null;
        }
        return responses.success(id, result);
    }

    private JsonNode dispatchMethod(String method, JsonNode params, McpCallContext context) {
        return methodControllers.stream()
                .filter(controller -> controller.supports(method))
                .findFirst()
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found"))
                .dispatch(method, params, context);
    }

    private void validateUniqueMethodOwners(List<McpMethodController> controllers) {
        Map<String, McpMethodController> owners = new LinkedHashMap<>();
        for (McpMethodController controller : controllers) {
            for (String method : controller.supportedMethods()) {
                McpMethodController existing = owners.putIfAbsent(method, controller);
                if (existing != null) {
                    throw new IllegalStateException("Duplicate MCP method mapping '%s' owned by %s and %s"
                            .formatted(method, existing.getClass().getName(), controller.getClass().getName()));
                }
            }
        }
    }

}
