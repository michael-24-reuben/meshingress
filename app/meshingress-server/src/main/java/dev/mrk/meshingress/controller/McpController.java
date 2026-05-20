package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcResponses;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final ObjectMapper objectMapper;
    private final McpDispatcher dispatcher;
    private final JsonRpcResponses responses;

    public McpController(ObjectMapper objectMapper, McpDispatcher dispatcher, JsonRpcResponses responses) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.responses = responses;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> post(
            @RequestBody String body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Mcp-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-Mcp-Admin", required = false) String legacyAdminHeader,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        JsonNode request;
        try {
            request = objectMapper.readTree(body);
        } catch (JacksonException exception) {
            return ResponseEntity.ok(responses.error(null, JsonRpcErrorCodes.PARSE_ERROR, "Parse error"));
        }

        McpCallContext context = new McpCallContext(authorization, roleHeader == null ? legacyAdminHeader : roleHeader, sessionId, requestId);
        Optional<JsonNode> response = dispatcher.dispatch(request, context);
        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<Void> get() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        return ResponseEntity.accepted().build();
    }


}
