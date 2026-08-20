package dev.mrk.meshingress.mcp.docs.post;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reusable OpenAPI @RequestBody definition for the MCP POST endpoint.
 * This annotation can be placed on the controller method in place of the
 * long inline @RequestBody declaration.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RequestBody(
        required = true,
        description = "JSON-RPC 2.0 payload. Use a single request object for ordinary calls, omit id for notifications, or send an array of request objects for batches.",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(oneOf = {
                        McpOpenApiSchemas.InitializeRequest.class,
                        McpOpenApiSchemas.InitializedNotification.class,
                        McpOpenApiSchemas.PingRequest.class,

                        McpOpenApiSchemas.ToolsListRequest.class,
                        McpOpenApiSchemas.ToolsCallRequest.class,

                        McpOpenApiSchemas.RolesToolsCheckRequest.class,
                        McpOpenApiSchemas.RolesToolsRegisterRequest.class,
                        McpOpenApiSchemas.RolesToolsInstallPublicationRequest.class,
                        McpOpenApiSchemas.RolesToolsAliasRequest.class,
                        McpOpenApiSchemas.RolesToolsUpdateRequest.class,
                        McpOpenApiSchemas.RolesToolsDeleteRequest.class,
                        McpOpenApiSchemas.RolesToolsListRequest.class,
                        McpOpenApiSchemas.RolesToolsReloadRequest.class,

                        McpOpenApiSchemas.JsonRpcBatchRequest.class
                }),
                examples = {
                        @ExampleObject(
                                name = "tools/list",
                                summary = "List public tools",
                                value = """
                                        {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
                                        """
                        ),
                        @ExampleObject(
                                name = "tools/call",
                                summary = "Invoke a tool",
                                value = """
                                        {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"helloworld.greeting.greet","arguments":{"name":"Meshingress"}}}
                                        """
                        ),
                        @ExampleObject(
                                name = "roles/tools/register",
                                summary = "Register a phase-aware local JAR tool",
                                value = """
                                        {"jsonrpc":"2.0","id":3,"method":"roles/tools/register","params":{"phase":"install","toolId":"sample.local","localJar":{"path":"C:/tools/sample.jar","checksumSha256":"<sha256>"},"replace":false}}
                                        """
                        ),
                        @ExampleObject(
                                name = "batch",
                                summary = "Batch request",
                                value = """
                                        [{"jsonrpc":"2.0","id":4,"method":"ping"},{"jsonrpc":"2.0","id":5,"method":"tools/list","params":{}}]
                                        """
                        )
                }
        )
)
public @interface McpOpenApiRequestBody {
}

