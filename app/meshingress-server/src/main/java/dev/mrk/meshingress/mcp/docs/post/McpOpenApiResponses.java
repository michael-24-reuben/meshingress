package dev.mrk.meshingress.mcp.docs.post;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "JSON-RPC response object or response batch. JSON-RPC method failures are represented in the response body error object.",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = McpOpenApiSchemas.JsonRpcResponse.class)
                )
        ),
        @ApiResponse(responseCode = "204", description = "Notification-only request or batch produced no JSON-RPC response."),
        @ApiResponse(responseCode = "415", description = "Unsupported media type.")
})
public @interface McpOpenApiResponses {
}
