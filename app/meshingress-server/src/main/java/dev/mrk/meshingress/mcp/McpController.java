package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpHttpMethod;
import dev.mrk.meshingress.route.annotations.McpRoute;
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

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpController.class);

    private final McpTransportDispatcher transportDispatcher;

    public McpController(McpTransportDispatcher transportDispatcher) {
        this.transportDispatcher = transportDispatcher;
    }

    @McpRoute(id = "mcp.transport.post.v1", method = McpHttpMethod.POST, path = "/mcp")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> post(
            @RequestBody String body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Mcp-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-Mcp-Admin", required = false) String legacyAdminHeader,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        LOGGER.info("=== MCP REQUEST START [http] requestId={} sessionId={} ===", requestId, sessionId);
        McpCallContext context = new McpCallContext(authorization, roleHeader == null ? legacyAdminHeader : roleHeader, sessionId, requestId);
        Optional<JsonNode> response = transportDispatcher.dispatch(body, context);
        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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
