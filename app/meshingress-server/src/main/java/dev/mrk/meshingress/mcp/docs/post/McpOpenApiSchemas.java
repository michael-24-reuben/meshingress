package dev.mrk.meshingress.mcp.docs.post;

import dev.mrk.meshingress.controller.roles.params.RolesToolAliasParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolCheckParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolDeleteParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolInstallPublicationParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolListParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolUpdateParams;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationParams;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Map;

/**
 * Documentation-only schemas for the JSON-RPC envelopes accepted by {@code POST /mcp}.
 * Runtime parsing remains in {@code McpTransportDispatcher} so batches and notifications
 * continue to use the same transport path as ordinary requests.
 */
public final class McpOpenApiSchemas {

    private McpOpenApiSchemas() {
    }

    @Schema(name = "McpJsonRpcBatchRequest", description = "JSON-RPC 2.0 batch request containing one or more request or notification objects.")
    public static final class JsonRpcBatchRequest extends ArrayList<JsonRpcRequest> {
    }

    @Schema(name = "McpJsonRpcRequest", description = "Generic JSON-RPC 2.0 request accepted by the MCP transport.")
    public record JsonRpcRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier. Omit this field for JSON-RPC notifications.", example = "1")
            Object id,
            @Schema(description = "MCP method name routed by the server dispatcher.", example = "tools/list", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(description = "Method-specific parameters. See the method-specific request schemas for concrete shapes.", oneOf = {
                    InitializeParams.class,
                    ToolsCallParams.class,
                    RolesToolCheckParams.class,
                    ToolRegistrationParams.class,
                    RolesToolInstallPublicationParams.class,
                    RolesToolAliasParams.class,
                    RolesToolUpdateParams.class,
                    RolesToolDeleteParams.class,
                    RolesToolListParams.class
            })
            Object params
    ) {
    }

    @Schema(name = "McpJsonRpcResponse", description = "Generic JSON-RPC 2.0 response returned by the MCP transport.")
    public record JsonRpcResponse(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0")
            String jsonrpc,
            @Schema(description = "The request identifier, or null when JSON-RPC requires it.", example = "1")
            Object id,
            @Schema(description = "Successful method result. The concrete shape depends on the invoked MCP method.", implementation = Object.class)
            Object result,
            @Schema(description = "JSON-RPC error object when the method cannot be completed.", implementation = Object.class)
            Object error
    ) {
    }

    @Schema(name = "McpInitializeRequest", description = "Initializes an MCP session and returns server capabilities.")
    public record InitializeRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "initialize", example = "initialize", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            InitializeParams params
    ) {
    }

    @Schema(name = "McpInitializedNotification", description = "Notification sent after the client accepts initialization. Notifications omit id and do not produce a response body.")
    public record InitializedNotification(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "MCP method name.", allowableValues = "notifications/initialized", example = "notifications/initialized", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(description = "Optional notification params.", implementation = Object.class)
            Object params
    ) {
    }

    @Schema(name = "McpPingRequest", description = "Health-style JSON-RPC ping routed through the MCP dispatcher.")
    public record PingRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "ping", example = "ping", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(description = "Optional ping params.", implementation = Object.class)
            Object params
    ) {
    }

    @Schema(name = "McpToolsListRequest", description = "Lists public enabled MCP tools.")
    public record ToolsListRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "tools/list", example = "tools/list", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(description = "Optional empty object.", implementation = Object.class, example = "{}")
            Object params
    ) {
    }

    @Schema(name = "McpToolsCallRequest", description = "Invokes a public MCP tool by function name.")
    public record ToolsCallRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "tools/call", example = "tools/call", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            ToolsCallParams params
    ) {
    }

    @Schema(name = "McpRolesToolsCheckRequest", description = "Admin request that validates a proposed dynamic tool descriptor.")
    public record RolesToolsCheckRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/check", example = "roles/tools/check", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RolesToolCheckParams params
    ) {
    }

    @Schema(name = "McpRolesToolsRegisterRequest", description = "Admin request that registers a phase-aware runtime tool.")
    public record RolesToolsRegisterRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/register", example = "roles/tools/register", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            ToolRegistrationParams params
    ) {
    }

    @Schema(name = "McpRolesToolsInstallPublicationRequest", description = "Admin request that installs a signed artifact publication and registers its tool.")
    public record RolesToolsInstallPublicationRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/installPublication", example = "roles/tools/installPublication", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RolesToolInstallPublicationParams params
    ) {
    }

    @Schema(name = "McpRolesToolsAliasRequest", description = "Admin request that registers a dynamic alias for a tool descriptor.")
    public record RolesToolsAliasRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/alias", example = "roles/tools/alias", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RolesToolAliasParams params
    ) {
    }

    @Schema(name = "McpRolesToolsUpdateRequest", description = "Admin request that applies a patch to an existing registry tool.")
    public record RolesToolsUpdateRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/update", example = "roles/tools/update", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RolesToolUpdateParams params
    ) {
    }

    @Schema(name = "McpRolesToolsDeleteRequest", description = "Admin request that disables a registry tool.")
    public record RolesToolsDeleteRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/delete", example = "roles/tools/delete", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RolesToolDeleteParams params
    ) {
    }

    @Schema(name = "McpRolesToolsListRequest", description = "Admin request that lists role-visible tools, audit events, and registrations.")
    public record RolesToolsListRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/list", example = "roles/tools/list", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RolesToolListParams params
    ) {
    }

    @Schema(name = "McpRolesToolsReloadRequest", description = "Admin request that reports current runtime reload support and registration state.")
    public record RolesToolsReloadRequest(
            @Schema(description = "JSON-RPC protocol version.", allowableValues = "2.0", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
            String jsonrpc,
            @Schema(description = "Client-supplied request identifier.", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
            Object id,
            @Schema(description = "MCP method name.", allowableValues = "roles/tools/reload", example = "roles/tools/reload", requiredMode = Schema.RequiredMode.REQUIRED)
            String method,
            @Schema(description = "Optional empty object.", implementation = Object.class, example = "{}")
            Object params
    ) {
    }

    @Schema(name = "McpInitializeParams", description = "MCP initialize parameters.")
    public record InitializeParams(
            @Schema(description = "Protocol version requested by the client.", example = "2025-11-25")
            String protocolVersion,
            @Schema(description = "Client capabilities object.", implementation = Object.class, example = "{}")
            Object capabilities,
            @Schema(description = "Client identity.")
            ClientInfo clientInfo
    ) {
    }

    @Schema(name = "McpClientInfo", description = "MCP client identity metadata.")
    public record ClientInfo(
            @Schema(description = "Client name.", example = "test-client")
            String name,
            @Schema(description = "Client version.", example = "0.1.0")
            String version
    ) {
    }

    @Schema(name = "McpToolsCallParams", description = "Parameters for tools/call.")
    public record ToolsCallParams(
            @Schema(description = "Name of the MCP tool function to invoke.", example = "helloworld.greeting.greet", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @Schema(description = "Tool-specific arguments object. Its schema is supplied by tools/list for each tool.", implementation = Map.class, example = "{\"name\":\"Meshingress\"}")
            Map<String, Object> arguments
    ) {
    }
}
