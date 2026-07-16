

# McpRolesToolsUpdateRequest

Admin request that applies a patch to an existing registry tool.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**id** | **Object** |  |  |
|**method** | [**MethodEnum**](#MethodEnum) | MCP method name. |  |
|**params** | [**RolesToolUpdateParams**](RolesToolUpdateParams.md) |  |  |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



## Enum: MethodEnum

| Name | Value |
|---- | -----|
| ROLES_TOOLS_UPDATE | &quot;roles/tools/update&quot; |



