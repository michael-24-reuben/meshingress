package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.mcp.docs.post.McpOpenApiRequestBody;
import dev.mrk.meshingress.mcp.docs.post.McpOpenApiResponses;
import dev.mrk.meshingress.route.annotations.McpHttpMethod;
import dev.mrk.meshingress.route.annotations.McpRoute;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpController.class);

    private final McpTransportDispatcher transportDispatcher;

    public McpController(McpTransportDispatcher transportDispatcher) {
        this.transportDispatcher = transportDispatcher;
    }

    @McpRoute(id = "mcp.transport.post.v1", method = McpHttpMethod.POST, path = "/mcp")
    @Operation(
            summary = "Dispatch MCP JSON-RPC requests",
            description = "Accepts JSON-RPC 2.0 single requests, notifications, and batches for the Meshingress MCP transport."
    )
    @McpOpenApiRequestBody
    @McpOpenApiResponses
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> post(
            @RequestBody String body,

            @Parameter(description = "Bearer admin token for role-gated MCP methods.", example = "Bearer dev-admin")
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,

            @Parameter(description = "MCP role hint. Use admin for role-gated registry methods.", example = "admin")
            @RequestHeader(value = "X-Mcp-Role", required = false) String roleHeader,

            @Parameter(description = "Legacy admin flag accepted for compatibility.", example = "true")
            @RequestHeader(value = "X-Mcp-Admin", required = false) String legacyAdminHeader,

            @Parameter(description = "Client MCP session identifier.", example = "session-123")
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,

            @Parameter(description = "Caller request correlation identifier.", example = "req-123")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        String effectiveSessionId = sessionId == null || sessionId.isBlank()
                ? UUID.randomUUID().toString()
                : sessionId;
        McpCallContext context = new McpCallContext(authorization, roleHeader == null ? legacyAdminHeader : roleHeader, effectiveSessionId, requestId);
        Optional<JsonNode> response = transportDispatcher.dispatch(body, McpInvocationFactory.http(context));
        return response
                .map(payload -> ResponseEntity.ok()
                        .header("Mcp-Session-Id", effectiveSessionId)
                        .body(payload))
                .orElseGet(() -> ResponseEntity.noContent()
                        .header("Mcp-Session-Id", effectiveSessionId)
                        .build());
    }

    @McpRoute(id = "mcp.transport.get.v1", method = McpHttpMethod.GET, path = "/mcp")
    @GetMapping
    public ResponseEntity<Void> get() {
        LOGGER.info("=== MCP REQUEST START [http] method=GET ===");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @McpRoute(id = "mcp.transport.delete.v1", method = McpHttpMethod.DELETE, path = "/mcp")
    @DeleteMapping
    public ResponseEntity<Void> delete() {
        LOGGER.info("=== MCP REQUEST START [http] method=DELETE ===");
        return ResponseEntity.accepted().build();
    }
}
