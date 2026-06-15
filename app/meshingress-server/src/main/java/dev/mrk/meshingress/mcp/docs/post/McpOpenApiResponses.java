package dev.mrk.meshingress.mcp.docs.post;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
        @ApiResponse(responseCode = "200", description = "JSON-RPC response object or response batch. JSON-RPC method failures are represented in the response body error object."),
        @ApiResponse(responseCode = "204", description = "Notification-only request or batch produced no JSON-RPC response."),
        @ApiResponse(responseCode = "415", description = "Unsupported media type.")
})
public @interface McpOpenApiResponses {
}
