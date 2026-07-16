

# McpJsonRpcRequest

Generic JSON-RPC 2.0 request accepted by the MCP transport.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**id** | **Object** |  |  [optional] |
|**method** | **String** | MCP method name routed by the server dispatcher. |  |
|**params** | [**McpJsonRpcRequestParams**](McpJsonRpcRequestParams.md) |  |  [optional] |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



