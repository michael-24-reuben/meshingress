

# McpToolsCallRequest

Invokes a public MCP tool by function name.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**id** | **Object** |  |  |
|**method** | [**MethodEnum**](#MethodEnum) | MCP method name. |  |
|**params** | [**McpToolsCallParams**](McpToolsCallParams.md) |  |  |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



## Enum: MethodEnum

| Name | Value |
|---- | -----|
| TOOLS_CALL | &quot;tools/call&quot; |



