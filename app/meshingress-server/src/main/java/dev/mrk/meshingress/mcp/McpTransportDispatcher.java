package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.controller.McpDispatcher;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcResponses;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
public class McpTransportDispatcher {

    private final ObjectMapper objectMapper;
    private final McpDispatcher dispatcher;
    private final JsonRpcResponses responses;

    public McpTransportDispatcher(ObjectMapper objectMapper, McpDispatcher dispatcher, JsonRpcResponses responses) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.responses = responses;
    }

    public Optional<JsonNode> dispatch(String payload, McpCallContext context) {
        JsonNode request;
        try {
            request = objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            return Optional.of(responses.error(null, JsonRpcErrorCodes.PARSE_ERROR, "Parse error"));
        }
        return dispatcher.dispatch(request, context);
    }
}
