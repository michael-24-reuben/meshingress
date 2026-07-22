

# McpRolesToolsCheckRequest

Admin request that validates a proposed dynamic tool descriptor.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**id** | **Object** |  |  |
|**method** | [**MethodEnum**](#MethodEnum) | MCP method name. |  |
|**params** | [**RolesToolCheckParams**](RolesToolCheckParams.md) |  |  |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



## Enum: MethodEnum

| Name | Value |
|---- | -----|
| ROLES_TOOLS_CHECK | &quot;roles/tools/check&quot; |



